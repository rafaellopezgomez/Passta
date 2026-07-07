package es.uma.morse.passta.io;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;

public class SvgViewer {

    public static void openSvgInBrowser(String svg) throws IOException {
        svg = svg.replaceFirst("(?s)<\\?xml.*?\\?>", "");

        String html = buildHtml(svg);

        openHtmlInLocalBrowser(html);
    }

    private static void openHtmlInLocalBrowser(String html) throws IOException {
        byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");

            exchange.sendResponseHeaders(200, htmlBytes.length);

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(htmlBytes);
            }
        });

        server.setExecutor(Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "passta-svg-viewer");
            thread.setDaemon(false);
            return thread;
        }));

        server.start();

        URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));

        openUri(uri);

        System.out.println("PASSTA viewer opened at: " + uri);

        waitBeforeStopping(server);
    }

    private static void waitBeforeStopping(HttpServer server) {
        if (System.console() != null) {
            System.out.println("Press ENTER to stop the PASSTA viewer server...");
            try {
                System.console().readLine();
            } finally {
                server.stop(0);
            }
            return;
        }

        Thread autoStopThread = new Thread(() -> {
            try {
                TimeUnit.MINUTES.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                server.stop(0);
            }
        }, "passta-viewer-auto-stop");

        autoStopThread.setDaemon(false);
        autoStopThread.start();
    }
    
    private static String buildHtml(String svg) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <style>
                body {
                  margin: 0;
                  overflow: hidden;
                  font-family: sans-serif;
                }

                svg {
                  width: 100vw;
                  height: 100vh;
                  background: #fafafa;
                  cursor: grab;
                }

                #controls {
                  position: absolute;
                  top: 10px;
                  left: 10px;
                  background: white;
                  padding: 5px;
                  border-radius: 5px;
                  box-shadow: 0 0 5px rgba(0,0,0,0.3);
                  z-index: 100;
                }

                #controls button {
                  font-size: 16px;
                  margin: 2px;
                }

                #edgeTooltip {
                  position: fixed;
                  display: none;
                  background: #222;
                  color: white;
                  padding: 8px 10px;
                  border-radius: 6px;
                  font-size: 13px;
                  pointer-events: none;
                  z-index: 9999;
                  max-width: 460px;
                  white-space: pre-line;
                  box-shadow: 0 2px 8px rgba(0,0,0,0.35);
                }
              </style>
            </head>

            <body>
              <div id="controls">
                <button id="zoomIn">+</button>
                <button id="zoomOut">–</button>
              </div>

              <div id="edgeTooltip"></div>

              %s

              <script>
                const svg = document.querySelector("svg");
                const tooltip = document.getElementById("edgeTooltip");

                const zoomFactor = 1.15;
                let isPanning = false;
                let start = {};

                if (!svg.getAttribute("viewBox")) {
                  svg.setAttribute("viewBox", "0 0 1000 600");
                }

                const vb = svg.getAttribute("viewBox").split(/\\s+/).map(Number);
                const viewBox = {
                  x: vb[0],
                  y: vb[1],
                  w: vb[2],
                  h: vb[3]
                };

                function updateViewBox() {
                  svg.setAttribute("viewBox", `${viewBox.x} ${viewBox.y} ${viewBox.w} ${viewBox.h}`);
                }

                function zoom(factor, mx, my) {
                  const newW = viewBox.w * factor;
                  const newH = viewBox.h * factor;

                  viewBox.x += (viewBox.w - newW) * mx;
                  viewBox.y += (viewBox.h - newH) * my;
                  viewBox.w = newW;
                  viewBox.h = newH;

                  updateViewBox();
                }

                document.getElementById("zoomIn").addEventListener("click", () => {
                  zoom(1 / zoomFactor, 0.5, 0.5);
                });

                document.getElementById("zoomOut").addEventListener("click", () => {
                  zoom(zoomFactor, 0.5, 0.5);
                });

                svg.addEventListener("wheel", e => {
                  e.preventDefault();

                  const rect = svg.getBoundingClientRect();
                  const factor = e.deltaY > 0 ? zoomFactor : 1 / zoomFactor;
                  const mx = (e.clientX - rect.left) / rect.width;
                  const my = (e.clientY - rect.top) / rect.height;

                  zoom(factor, mx, my);
                }, { passive: false });

                svg.addEventListener("mousedown", e => {
                  if (e.button !== 0) return;
                  if (e.target.closest("#edge-hover-overlay")) return;

                  isPanning = true;
                  start = { x: e.clientX, y: e.clientY };
                  svg.style.cursor = "grabbing";
                });

                window.addEventListener("mouseup", () => {
                  isPanning = false;
                  svg.style.cursor = "grab";
                });

                window.addEventListener("mousemove", e => {
                  if (!isPanning) return;

                  const dx = (e.clientX - start.x) * viewBox.w / svg.clientWidth;
                  const dy = (e.clientY - start.y) * viewBox.h / svg.clientHeight;

                  viewBox.x -= dx;
                  viewBox.y -= dy;

                  start = { x: e.clientX, y: e.clientY };
                  updateViewBox();
                });

                function normalizeText(text) {
                  return text
                    .replace(new RegExp(String.fromCharCode(160), "g"), " ")
                    .replace(/<=/g, "≤")
                    .replace(/->/g, " → ")
                    .trim();
                }

                function showTooltip(event, text) {
                  tooltip.textContent = normalizeText(text);
                  tooltip.style.display = "block";
                  tooltip.style.left = event.clientX + 14 + "px";
                  tooltip.style.top = event.clientY + 14 + "px";
                }

                function hideTooltip() {
                  tooltip.style.display = "none";
                }

                function getLineInfo(lineGroup) {
                  const title = lineGroup.querySelector("title")?.textContent.trim();
                  const labels = Array.from(lineGroup.querySelectorAll("text"))
                    .map(text => text.textContent.trim())
                    .filter(Boolean);

                  return [title, ...labels]
                    .filter(Boolean)
                    .join("\\n");
                }

                function enhanceEdges() {
                  const graphRoot = svg.querySelector("#graph_root") || svg;
                  const overlay = document.createElementNS("http://www.w3.org/2000/svg", "g");

                  overlay.setAttribute("id", "edge-hover-overlay");
                  graphRoot.appendChild(overlay);

                  svg.querySelectorAll('g[id^="line_"]').forEach(lineGroup => {
                    const path = lineGroup.querySelector("path");
                    if (!path) return;

                    const hoverPath = path.cloneNode(false);

                    hoverPath.removeAttribute("id");
                    hoverPath.setAttribute("stroke", "#000000");
                    hoverPath.setAttribute("stroke-width", "24");
                    hoverPath.setAttribute("stroke-opacity", "0.001");
                    hoverPath.setAttribute("fill", "none");
                    hoverPath.setAttribute("pointer-events", "stroke");
                    hoverPath.setAttribute("cursor", "pointer");

                    const info = getLineInfo(lineGroup);

                    hoverPath.addEventListener("mouseenter", e => showTooltip(e, info));
                    hoverPath.addEventListener("mousemove", e => showTooltip(e, info));
                    hoverPath.addEventListener("mouseleave", hideTooltip);

                    overlay.appendChild(hoverPath);
                  });
                }

                enhanceEdges();
                updateViewBox();
              </script>
            </body>
            </html>
            """.formatted(svg);
    }

    private static void openUri(URI uri) throws IOException {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(uri);
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            new ProcessBuilder("cmd", "/c", "start", "", uri.toString()).start();
        } else if (os.contains("mac")) {
            new ProcessBuilder("open", uri.toString()).start();
        } else {
            new ProcessBuilder("xdg-open", uri.toString()).start();
        }
    }
}