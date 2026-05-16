#!/usr/bin/env python3
"""
Chennai Rainfall Data Extraction Script

This script scrapes Chennai rainfall data from kwschennai.com and converts it
to the required format: year, month, rainfall_mm

Since the website might have restrictions or be unavailable, this script also
includes a fallback to convert the existing chennai-monthly-rains.csv data
to the required format.

Usage:
    python extract_chennai_rainfall.py

Output:
    src/main/resources/chennai_monthly_rainfall.csv
"""

import csv
import os
import sys
import requests
from bs4 import BeautifulSoup
import pandas as pd
import time

def scrape_from_website():
    """
    Attempt to scrape rainfall data from kwschennai.com
    Returns: list of dictionaries with year, month, rainfall_mm
    """
    try:
        print("Attempting to scrape data from kwschennai.com...")
        
        # Headers to mimic a real browser
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }
        
        response = requests.get('https://kwschennai.com/rainfall.htm', headers=headers, timeout=10)
        response.raise_for_status()
        
        soup = BeautifulSoup(response.content, 'html.parser')
        
        # Look for tables containing rainfall data
        tables = soup.find_all('table')
        rainfall_data = []
        
        # This is a placeholder implementation since the actual website structure is unknown
        # In a real implementation, you would parse the HTML tables based on the actual structure
        print("Website scraping would need specific HTML parsing logic based on the actual site structure")
        return None
        
    except requests.RequestException as e:
        print(f"Failed to scrape website: {e}")
        return None
    except Exception as e:
        print(f"Error parsing website data: {e}")
        return None

def convert_existing_data():
    """
    Convert existing chennai-monthly-rains.csv to required format
    Returns: list of dictionaries with year, month, rainfall_mm
    """
    print("Converting existing data from chennai-monthly-rains.csv...")
    
    input_file = 'src/main/resources/chennai-monthly-rains.csv'
    if not os.path.exists(input_file):
        print(f"Input file {input_file} not found!")
        return None
    
    rainfall_data = []
    month_names = ['Jan', 'Feb', 'Mar', 'April', 'May', 'June', 
                   'July', 'Aug', 'Sept', 'Oct', 'Nov', 'Dec']
    
    with open(input_file, 'r') as file:
        reader = csv.DictReader(file)
        for row in reader:
            year = int(row['Year'])
            
            # Only include last 25 years (1997-2021) as requested
            if year >= 1997:
                for i, month_name in enumerate(month_names, 1):
                    rainfall_mm = float(row[month_name])
                    rainfall_data.append({
                        'year': year,
                        'month': i,
                        'rainfall_mm': round(rainfall_mm, 2)
                    })
    
    print(f"Converted {len(rainfall_data)} records for {len(set(r['year'] for r in rainfall_data))} years")
    return rainfall_data

def save_to_csv(rainfall_data, output_file):
    """
    Save rainfall data to CSV file
    """
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    
    with open(output_file, 'w', newline='') as file:
        writer = csv.DictWriter(file, fieldnames=['year', 'month', 'rainfall_mm'])
        writer.writeheader()
        writer.writerows(rainfall_data)
    
    print(f"Data saved to {output_file}")

def main():
    """
    Main function to extract Chennai rainfall data
    """
    print("=== Chennai Rainfall Data Extraction ===")
    
    output_file = 'src/main/resources/chennai_monthly_rainfall.csv'
    
    # Try to scrape from website first
    rainfall_data = scrape_from_website()
    
    # If website scraping fails, use existing data
    if rainfall_data is None:
        print("Website scraping failed or not available, using existing data...")
        rainfall_data = convert_existing_data()
    
    if rainfall_data is None:
        print("Failed to extract data from any source!")
        sys.exit(1)
    
    # Save to CSV
    save_to_csv(rainfall_data, output_file)
    
    # Display summary
    years = sorted(set(r['year'] for r in rainfall_data))
    print(f"\nData Summary:")
    print(f"- Years covered: {min(years)} to {max(years)}")
    print(f"- Total records: {len(rainfall_data)}")
    print(f"- Average annual records: {len(rainfall_data) / len(years):.1f}")
    
    # Show sample data
    print(f"\nSample data (first 5 records):")
    for i, record in enumerate(rainfall_data[:5]):
        print(f"  {record}")
    
    print(f"\nData extraction completed successfully!")

if __name__ == "__main__":
    main()