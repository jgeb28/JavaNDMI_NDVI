import os
import subprocess
import sys

venv_path = "./venv"

if not os.path.isdir(venv_path):
    print("Creating virtual environment")
    subprocess.check_call([sys.executable, "-m", "venv", venv_path])
else:
    print("Virtual environment already exists.")

if os.name == 'nt':
    pip_path = os.path.join(venv_path, "Scripts", "pip.exe")
else:
    pip_path = os.path.join(venv_path, "bin", "pip")

print("Installing packages")
subprocess.check_call([pip_path, "install", "rasterio"])

print("Setup complete.")
