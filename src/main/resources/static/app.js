/**
 * Dashboard for the URL Shortener API.
 *
 * The API intentionally has no "list all links" endpoint (a short link is only
 * meaningful to whoever created it), so this dashboard remembers the links you
 * create in this browser via localStorage and refreshes their click counts from
 * GET /api/v1/urls/{code}. All calls are same-origin against the hosting app.
 */
(() => {
    "use strict";

    const STORAGE_KEY = "snip.links.v1";
    const API = "/api/v1/urls";

    // ---- DOM refs -------------------------------------------------------------
    const form = document.getElementById("create-form");
    const urlInput = document.getElementById("url-input");
    const createBtn = document.getElementById("create-btn");
    const btnLabel = createBtn.querySelector(".btn-label");
    const spinner = createBtn.querySelector(".spinner");
    const formError = document.getElementById("form-error");
    const coldNote = document.getElementById("cold-note");

    const result = document.getElementById("result");
    const resultShort = document.getElementById("result-short");
    const resultOriginal = document.getElementById("result-original");
    const copyBtn = document.getElementById("copy-btn");
    const openBtn = document.getElementById("open-btn");

    const statLinks = document.getElementById("stat-links");
    const statClicks = document.getElementById("stat-clicks");
    const statTop = document.getElementById("stat-top");

    const emptyState = document.getElementById("empty-state");
    const tableWrap = document.getElementById("table-wrap");
    const linksBody = document.getElementById("links-body");
    const refreshBtn = document.getElementById("refresh-btn");
    const clearBtn = document.getElementById("clear-btn");

    const healthPill = document.getElementById("health-pill");
    const healthText = document.getElementById("health-text");
    const toast = document.getElementById("toast");

    // ---- storage --------------------------------------------------------------
    /** @returns {Array<{shortCode,shortUrl,originalUrl,hitCount,createdAt}>} */
    function loadLinks() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            return raw ? JSON.parse(raw) : [];
        } catch {
            return [];
        }
    }

    function saveLinks(links) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(links));
    }

    let links = loadLinks();

    // ---- helpers --------------------------------------------------------------
    function escapeHtml(str) {
        return String(str).replace(/[&<>"']/g, (c) => ({
            "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
        }[c]));
    }

    function relativeTime(iso) {
        const then = new Date(iso).getTime();
        if (Number.isNaN(then)) return "";
        const secs = Math.round((Date.now() - then) / 1000);
        if (secs < 45) return "just now";
        const mins = Math.round(secs / 60);
        if (mins < 60) return `${mins}m ago`;
        const hrs = Math.round(mins / 60);
        if (hrs < 24) return `${hrs}h ago`;
        const days = Math.round(hrs / 24);
        if (days < 30) return `${days}d ago`;
        return new Date(iso).toLocaleDateString();
    }

    function showToast(msg) {
        toast.textContent = msg;
        toast.hidden = false;
        // reflow so the transition re-triggers on rapid successive toasts
        void toast.offsetWidth;
        toast.classList.add("show");
        clearTimeout(showToast._t);
        showToast._t = setTimeout(() => {
            toast.classList.remove("show");
            setTimeout(() => { toast.hidden = true; }, 250);
        }, 1900);
    }

    async function copyText(text) {
        try {
            await navigator.clipboard.writeText(text);
            showToast("Copied to clipboard");
        } catch {
            // Fallback for non-secure contexts (e.g. plain http on a LAN IP).
            const ta = document.createElement("textarea");
            ta.value = text;
            ta.style.position = "fixed";
            ta.style.opacity = "0";
            document.body.appendChild(ta);
            ta.select();
            try { document.execCommand("copy"); showToast("Copied to clipboard"); }
            catch { showToast("Copy failed — select and copy manually"); }
            document.body.removeChild(ta);
        }
    }

    // ---- rendering ------------------------------------------------------------
    function renderStats() {
        const totalClicks = links.reduce((sum, l) => sum + (l.hitCount || 0), 0);
        statLinks.textContent = links.length;
        statClicks.textContent = totalClicks;

        const top = links.reduce(
            (best, l) => (l.hitCount || 0) > (best?.hitCount || 0) ? l : best,
            null
        );
        statTop.textContent = top && top.hitCount > 0 ? `/${top.shortCode}` : "—";
    }

    function renderLinks() {
        if (links.length === 0) {
            emptyState.hidden = false;
            tableWrap.hidden = true;
            renderStats();
            return;
        }
        emptyState.hidden = true;
        tableWrap.hidden = false;

        // Newest first.
        const ordered = [...links].sort(
            (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
        );

        linksBody.innerHTML = ordered.map((l) => {
            const short = escapeHtml(l.shortUrl);
            const code = escapeHtml(l.shortCode);
            const original = escapeHtml(l.originalUrl);
            return `
                <tr data-code="${code}">
                    <td><a class="cell-short" href="${short}" target="_blank" rel="noopener">/${code}</a></td>
                    <td class="cell-dest"><a href="${original}" target="_blank" rel="noopener" title="${original}">${original}</a></td>
                    <td class="col-clicks"><span class="clicks-badge">${l.hitCount || 0}</span></td>
                    <td class="col-created muted">${relativeTime(l.createdAt)}</td>
                    <td class="col-actions">
                        <div class="row-actions">
                            <button class="icon-btn" data-act="copy" title="Copy short link">⧉</button>
                            <button class="icon-btn" data-act="stats" title="Refresh clicks">↻</button>
                            <button class="icon-btn danger" data-act="remove" title="Remove from this list">✕</button>
                        </div>
                    </td>
                </tr>`;
        }).join("");

        renderStats();
    }

    function upsertLink(link) {
        const idx = links.findIndex((l) => l.shortCode === link.shortCode);
        if (idx >= 0) links[idx] = { ...links[idx], ...link };
        else links.unshift(link);
        saveLinks(links);
        renderLinks();
    }

    // ---- API calls ------------------------------------------------------------
    function setLoading(on) {
        createBtn.disabled = on;
        spinner.hidden = !on;
        btnLabel.textContent = on ? "Shortening…" : "Shorten";
    }

    async function createLink(url) {
        formError.hidden = true;
        setLoading(true);
        // A cold free-tier instance can take a while — hint after a short delay.
        const coldTimer = setTimeout(() => { coldNote.hidden = false; }, 2500);

        try {
            const res = await fetch(API, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ url }),
            });

            if (!res.ok) {
                let msg = `Request failed (${res.status})`;
                try {
                    const err = await res.json();
                    if (err && Array.isArray(err.messages) && err.messages.length) {
                        msg = err.messages.join("; ");
                    } else if (err && err.error) {
                        msg = err.error;
                    }
                } catch { /* non-JSON error body */ }
                throw new Error(msg);
            }

            const data = await res.json();
            upsertLink(data);

            resultShort.textContent = data.shortUrl;
            resultShort.href = data.shortUrl;
            resultOriginal.textContent = data.originalUrl;
            result.hidden = false;
            result.dataset.short = data.shortUrl;
            urlInput.value = "";
            showToast("Short link created");
        } catch (e) {
            formError.textContent = e.message || "Something went wrong.";
            formError.hidden = false;
        } finally {
            clearTimeout(coldTimer);
            coldNote.hidden = true;
            setLoading(false);
        }
    }

    async function refreshOne(code) {
        const res = await fetch(`${API}/${encodeURIComponent(code)}`);
        if (!res.ok) throw new Error(`stats ${res.status}`);
        return res.json();
    }

    async function refreshAll() {
        if (links.length === 0) return;
        refreshBtn.disabled = true;
        const original = refreshBtn.textContent;
        refreshBtn.textContent = "↻ Refreshing…";
        let gone = 0;

        const results = await Promise.allSettled(
            links.map((l) => refreshOne(l.shortCode))
        );

        const fresh = [];
        results.forEach((r, i) => {
            if (r.status === "fulfilled") {
                fresh.push({ ...links[i], ...r.value });
            } else if (/\b404\b/.test(r.reason?.message || "")) {
                // The server restarted / link expired — drop it from the list.
                gone += 1;
            } else {
                fresh.push(links[i]); // keep as-is on transient errors
            }
        });

        links = fresh;
        saveLinks(links);
        renderLinks();
        flashBadges();

        refreshBtn.disabled = false;
        refreshBtn.textContent = original;
        showToast(gone > 0
            ? `Clicks updated · ${gone} stale link(s) removed`
            : "Clicks updated");
    }

    function flashBadges() {
        linksBody.querySelectorAll(".clicks-badge").forEach((b) => {
            b.classList.remove("bump");
            void b.offsetWidth;
            b.classList.add("bump");
        });
    }

    // ---- health ---------------------------------------------------------------
    async function pollHealth() {
        try {
            const res = await fetch("/actuator/health", { cache: "no-store" });
            const data = await res.json();
            const up = res.ok && data.status === "UP";
            healthPill.className = `pill ${up ? "pill-up" : "pill-down"}`;
            healthText.textContent = up ? "API healthy" : (data.status || "degraded");
        } catch {
            healthPill.className = "pill pill-down";
            healthText.textContent = "unreachable";
        }
    }

    // ---- events ---------------------------------------------------------------
    form.addEventListener("submit", (e) => {
        e.preventDefault();
        const url = urlInput.value.trim();
        if (url) createLink(url);
    });

    copyBtn.addEventListener("click", () => {
        if (result.dataset.short) copyText(result.dataset.short);
    });
    openBtn.addEventListener("click", () => {
        if (result.dataset.short) window.open(result.dataset.short, "_blank", "noopener");
    });

    linksBody.addEventListener("click", async (e) => {
        const btn = e.target.closest(".icon-btn");
        if (!btn) return;
        const row = btn.closest("tr");
        const code = row?.dataset.code;
        const link = links.find((l) => l.shortCode === code);
        if (!link) return;

        if (btn.dataset.act === "copy") {
            copyText(link.shortUrl);
        } else if (btn.dataset.act === "remove") {
            links = links.filter((l) => l.shortCode !== code);
            saveLinks(links);
            renderLinks();
            showToast("Removed from list");
        } else if (btn.dataset.act === "stats") {
            btn.textContent = "…";
            try {
                const data = await refreshOne(code);
                upsertLink(data);
                flashBadges();
                showToast(`/${code} · ${data.hitCount} click(s)`);
            } catch {
                showToast(`Couldn't refresh /${code}`);
                btn.textContent = "↻";
            }
        }
    });

    refreshBtn.addEventListener("click", refreshAll);

    clearBtn.addEventListener("click", () => {
        if (links.length === 0) return;
        if (!confirm("Remove all links from this browser? (Server data is untouched.)")) return;
        links = [];
        saveLinks(links);
        result.hidden = true;
        renderLinks();
        showToast("Cleared");
    });

    // ---- init -----------------------------------------------------------------
    renderLinks();
    pollHealth();
    setInterval(pollHealth, 20000);
    // Freshen click counts on load so returning users see current numbers.
    if (links.length > 0) refreshAll();
})();
