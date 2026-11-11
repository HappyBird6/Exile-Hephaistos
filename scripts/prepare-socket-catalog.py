"""Compatibility entrypoint for the read-only canonical Augment audit."""
from pathlib import Path
import runpy

runpy.run_path(str(Path(__file__).with_name("expand-augment-catalog.py")), run_name="__main__")
