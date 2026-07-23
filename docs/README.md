# Demo assets

Two visuals make the main README far more compelling. Generate them once, drop
them in this folder, then uncomment the image lines in the root `README.md`
(under the **📸 Screenshots** section).

## 1. `swagger.png` — the interactive API docs

1. Run the app locally: `mvn spring-boot:run`
2. Open http://localhost:8080/swagger-ui.html
3. Take a screenshot and save it as `docs/swagger.png`.

## 2. `demo.gif` — the API in action (one command)

This repo ships a [VHS](https://github.com/charmbracelet/vhs) tape that records
the create → redirect → stats flow automatically.

```bash
# one-time: install the recorder
brew install vhs        # macOS  (see the VHS repo for Linux)

# run the app in one terminal
mvn spring-boot:run

# in another terminal, from the repo root:
vhs docs/demo.tape      # produces docs/demo.gif
```

## Then enable them

In the root `README.md`, uncomment:

```markdown
![Swagger UI](docs/swagger.png)
![API demo](docs/demo.gif)
```
