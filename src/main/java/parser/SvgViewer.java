package parser;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SvgViewer {

    public static void openSvgInBrowser(String svg) throws IOException {
        svg = svg.replaceFirst("(?s)<\\?xml.*?\\?>", "");

        String html = buildHtml(svg);

        Path tmp = Files.createTempFile("automaton-", ".html");
        Files.writeString(tmp, html, StandardCharsets.UTF_8);
        tmp.toFile().deleteOnExit();

        openUri(tmp.toUri());
    }

    private static String buildHtml(String svg) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <style>
                body { margin:0; overflow:hidden; }
                svg { width:100vw; height:100vh; background:#fafafa; cursor:grab; }
                #controls {
                  position:absolute;
                  top:10px; left:10px;
                  background:white;
                  padding:5px;
                  border-radius:5px;
                  box-shadow:0 0 5px rgba(0,0,0,0.3);
                  z-index:100;
                  font-family: sans-serif;
                }
                #controls button { font-size:16px; margin:2px; }
              </style>
            </head>
            <body>
              <div id="controls">
                <button id="zoomIn">+</button>
                <button id="zoomOut">–</button>
              </div>

              %s

              <script>
                const svg = document.querySelector("svg");

                if (!svg.getAttribute("viewBox")) {
                  svg.setAttribute("viewBox", "0 0 1000 600");
                }

                const vb = svg.getAttribute("viewBox").split(/\\s+/).map(Number);
                let viewBox = { x: vb[0], y: vb[1], w: vb[2], h: vb[3] };

                const zoomFactor = 1.15;
                let isPanning = false;
                let start = {};

                function updateViewBox() {
                  svg.setAttribute("viewBox", `${viewBox.x} ${viewBox.y} ${viewBox.w} ${viewBox.h}`);
                }

                document.getElementById("zoomIn").addEventListener("click", ()=> zoom(1/zoomFactor,0.5,0.5));
                document.getElementById("zoomOut").addEventListener("click", ()=> zoom(zoomFactor,0.5,0.5));

                svg.addEventListener("wheel", e => {
                  e.preventDefault();
                  const dir = e.deltaY > 0 ? 1 : -1;
                  const factor = dir > 0 ? zoomFactor : 1/zoomFactor;
                  const mx = e.offsetX / svg.clientWidth;
                  const my = e.offsetY / svg.clientHeight;
                  zoom(factor, mx, my);
                }, { passive:false });

                function zoom(factor, mx, my) {
                  const newW = viewBox.w * factor;
                  const newH = viewBox.h * factor;
                  viewBox.x += (viewBox.w - newW) * mx;
                  viewBox.y += (viewBox.h - newH) * my;
                  viewBox.w = newW;
                  viewBox.h = newH;
                  updateViewBox();
                }

                svg.addEventListener("mousedown", e => {
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

                updateViewBox();
              </script>
            </body>
            </html>
            """.formatted(svg);
    }

    private static void openUri(URI uri) throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(uri);
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            new ProcessBuilder("cmd", "/c", "start", uri.toString()).start();
        } else if (os.contains("mac")) {
            new ProcessBuilder("open", uri.toString()).start();
        } else {
            new ProcessBuilder("xdg-open", uri.toString()).start();
        }
    }
}