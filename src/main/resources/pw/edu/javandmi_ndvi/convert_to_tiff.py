import os
import sys
import zipfile
import tempfile
from pathlib import Path
import rasterio
import shutil 

def extract_safe_zip(zip_path):
    temp_dir = tempfile.mkdtemp()
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall(temp_dir)
    return temp_dir

def find_band_file(root_dir, band_id):
    for path in Path(root_dir).rglob(f"*{band_id}*.jp2"):
        if "MSK" not in path.name:
            return path
    return None

def convert_jp2_to_tif(jp2_path, tiff_path=None):
    jp2_path = Path(jp2_path)
    if tiff_path is None:
        tiff_path = jp2_path.with_suffix('.tif')

    with rasterio.open(jp2_path) as src:
        profile = src.profile.copy()
        profile.update(driver='GTiff')
        data = src.read()
        with rasterio.open(tiff_path, 'w', **profile) as dst:
            dst.write(data)

    return tiff_path

def process_safe_zip(zip_path, output_dir):
    print("Start of conversion");
    temp_dir = extract_safe_zip(zip_path)
    
    bands = ['B04', 'B08', 'B11']
    for band in bands:
        jp2_file = find_band_file(temp_dir, band)
        if jp2_file:
            tiff_path = Path(output_dir) / f"{band}.tif"  
            convert_jp2_to_tif(jp2_file, tiff_path)
        else:
            print(f"Band {band} not found.")

    print("Cleaning up temporary files")
    shutil.rmtree(temp_dir)
    print("Conversion complete.")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(1)

    zip_file = sys.argv[1]
    output_folder = './converted_tifs'

    os.makedirs(output_folder, exist_ok=True)
    process_safe_zip(zip_file, output_folder)
