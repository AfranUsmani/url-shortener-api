package io.github.afranusmani.urlshortener.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAgentsTest {

    @Test
    void classifiesDesktopChrome() {
        var c = UserAgents.classify(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36");
        assertThat(c.device()).isEqualTo("Desktop");
        assertThat(c.browser()).isEqualTo("Chrome");
    }

    @Test
    void classifiesMobileSafari() {
        var c = UserAgents.classify(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605 Version/17.0 Mobile Safari/604");
        assertThat(c.device()).isEqualTo("Mobile");
        assertThat(c.browser()).isEqualTo("Safari");
    }

    @Test
    void classifiesTabletAndEdge() {
        var c = UserAgents.classify(
                "Mozilla/5.0 (iPad; CPU OS 17_0) AppleWebKit/605 (KHTML, like Gecko) Edg/125.0");
        assertThat(c.device()).isEqualTo("Tablet");
        assertThat(c.browser()).isEqualTo("Edge");
    }

    @Test
    void classifiesBots() {
        assertThat(UserAgents.classify("curl/8.4.0").device()).isEqualTo("Bot");
        assertThat(UserAgents.classify("Googlebot/2.1 (+http://www.google.com/bot.html)").device()).isEqualTo("Bot");
    }

    @Test
    void handlesMissingUserAgent() {
        var c = UserAgents.classify(null);
        assertThat(c.device()).isEqualTo("Unknown");
        assertThat(c.browser()).isEqualTo("Unknown");
    }

    @Test
    void reducesRefererToHostWithoutWww() {
        assertThat(UserAgents.referrerHost("https://www.twitter.com/a/b?x=1")).isEqualTo("twitter.com");
        assertThat(UserAgents.referrerHost("https://news.ycombinator.com/")).isEqualTo("news.ycombinator.com");
    }

    @Test
    void treatsMissingRefererAsDirect() {
        assertThat(UserAgents.referrerHost(null)).isEqualTo("Direct");
        assertThat(UserAgents.referrerHost("   ")).isEqualTo("Direct");
    }
}
