#!/usr/bin/env python3

import requests
import time
import os

# Create raw data directory
os.makedirs('/workspaces/clime/raw_weather_data', exist_ok=True)

base_url = "https://xyzxyzxyzxyz/summary/"
failed_downloads = []
successful_downloads = []

# Download data from 2005_09 to 2025_06
for year in range(2005, 2026):
    start_month = 9 if year == 2005 else 1
    end_month = 6 if year == 2025 else 12
    
    for month in range(start_month, end_month + 1):
        year_month = f"{year}_{month:02d}"
        url = f"{base_url}{year_month}.txt"
        filename = f"/workspaces/clime/raw_weather_data/{year_month}.txt"
        
        try:
            print(f"Downloading {year_month}...")
            response = requests.get(url, timeout=10)
            
            if response.status_code == 200:
                with open(filename, 'w', encoding='utf-8') as f:
                    f.write(response.text)
                successful_downloads.append(year_month)
                print(f"✅ {year_month} downloaded successfully")
            else:
                failed_downloads.append(f"{year_month} (HTTP {response.status_code})")
                print(f"❌ {year_month} failed: HTTP {response.status_code}")
                
        except Exception as e:
            failed_downloads.append(f"{year_month} ({str(e)})")
            print(f"❌ {year_month} failed: {e}")
        
        # Be nice to the server
        time.sleep(0.2)

print("\n" + "="*50)
print(f"✅ Successful downloads: {len(successful_downloads)}")
print(f"❌ Failed downloads: {len(failed_downloads)}")

if failed_downloads:
    print("\nFailed downloads:")
    for failed in failed_downloads:
        print(f"  - {failed}")

print(f"\nRaw data files saved in: /workspaces/clime/raw_weather_data/")
print("You can now inspect individual files and debug parsing issues.")
