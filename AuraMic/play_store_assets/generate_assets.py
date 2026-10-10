#!/usr/bin/env python3
"""Renders every Play Store graphic from its HTML/SVG source with headless Chromium (Playwright).

    pip install playwright pillow        # Chromium is reused if PLAYWRIGHT_BROWSERS_PATH is set
    python generate_assets.py

Outputs (next to this script):
    icon/app_icon_512.png                 512x512  32-bit PNG   (Play "App icon")
    feature_graphic/feature_graphic.png   1024x500 PNG          (Play "Feature graphic")
    screenshots/phone_01..08_*.png        1080x1920 PNG         (Play "Phone screenshots")
"""
import base64
import os
import pathlib
import sys

from playwright.sync_api import sync_playwright

ROOT = pathlib.Path(__file__).resolve().parent
SHOTS = ["hold_to_record", "noise_removal", "auto_send", "privacy", "setup", "languages", "advanced_controls", "chat_apps"]


def render(page, url, out, w, h, transparent=False):
    page.set_viewport_size({"width": w, "height": h})
    page.goto(url)
    page.wait_for_load_state("networkidle")
    page.evaluate("document.fonts.ready")
    page.screenshot(path=str(out), omit_background=transparent, clip={"x": 0, "y": 0, "width": w, "height": h})
    print("wrote", out.relative_to(ROOT), f"{w}x{h}")


def main():
    with sync_playwright() as p:
        # CHROMIUM_PATH lets you point at an existing Chromium/Chrome build instead of Playwright's own.
        exe = os.environ.get("CHROMIUM_PATH")
        browser = p.chromium.launch(executable_path=exe) if exe else p.chromium.launch()
        page = browser.new_page(device_scale_factor=1)

        # Icon: SVG -> PNG
        svg = (ROOT / "icon/app_icon.svg").resolve()
        render(page, svg.as_uri(), ROOT / "icon/app_icon_512.png", 512, 512)

        # Feature graphic: inline the icon as a data URI so the HTML is self-contained
        icon_b64 = base64.b64encode(svg.read_bytes()).decode()
        html = (ROOT / "feature_graphic/feature_graphic.html").read_text().replace(
            "__ICON__", f"data:image/svg+xml;base64,{icon_b64}")
        tmp = ROOT / "feature_graphic/.rendered.html"
        tmp.write_text(html)
        render(page, tmp.resolve().as_uri(), ROOT / "feature_graphic/feature_graphic.png", 1024, 500)
        tmp.unlink()

        # Screenshots
        page2 = browser.new_page(device_scale_factor=1)
        for i, name in enumerate(SHOTS, 1):
            render(page2, (ROOT / "screenshots/screens.html").resolve().as_uri() + f"#{i}",
                   ROOT / f"screenshots/phone_{i:02d}_{name}.png", 1080, 1920)
        browser.close()


if __name__ == "__main__":
    sys.exit(main())
