# 🌿 NDVI & NDMI Analyzer

A desktop application developed as part of the Parallel and Distributed Programming course project for analyzing Sentinel-2 satellite imagery using vegetation (NDVI) and moisture (NDMI) indices. Thanks to parallel data processing, the application allows for quick and interactive assessment of vegetation health over a selected area.

## 🚀 Launch Instructions

To run the application:

```
mvn javafx:run
```

or:

- Linux: `./mvnw javafx:run`
- Windows: `mvnw.cmd javafx:run`

## 🛠️ Technologies Used

- **Java 17**
- **JavaFX** – GUI
- **GeoTools** – spatial data handling
- **Rasterio (Python)** – conversion from `.jp2` to `.tif`
- **Concurrency API (ExecutorService)** – parallel processing
- **Maven** – project build and execution

## 📂 Features

- ✅ Load Sentinel-2 satellite data (.zip)
- ✅ Compute **NDVI** and **NDMI** indices
- ✅ Handle large images via parallel processing
- ✅ Pause and resume computations
- ✅ Save results as `.png` images
- ✅ Interactive GUI with progress updates

## 🔄 Processing Workflow

1. Load ZIP data → convert `.jp2` bands to `.tif` using a Python script
2. Rescale bands to match resolution
3. Parallel computation of NDVI and NDMI:

```
NDVI = (NIR - RED) / (NIR + RED)
NDMI = (NIR - SWIR) / (NIR + SWIR)
```

4. Generate output images

## 🧩 Sample Output

- 🌱 **NDVI** – healthy vegetation (green), poor vegetation (brown)

![NDVI result](./ndvi.png)

- 💧 **NDMI** – high moisture (blue), dry areas (orange)

![NDMI result](./ndmi.png)

Screenshots of actual output images were used to visualize the example results.
