#!/usr/bin/env python3

import os
import re
import csv
from collections import defaultdict

def detect_format_type(lines):
    """Detect which format type this file uses"""
    for line in lines:
        if "HEAT" in line and "COOL" in line and "DEG" in line:
            return "format_with_deg_days"
        elif "WIND SPEED" in line and "DOM" in line:
            return "standard_format"
    return "standard_format"

def parse_weather_file(filepath):
    """Parse a single weather file and return records"""
    records = []
    year_month = os.path.basename(filepath).replace('.txt', '')
    year, month = year_month.split('_')
    
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    format_type = detect_format_type(lines)
    print(f"  Format detected for {year_month}: {format_type}")
    
    data_started = False
    for line in lines:
        line = line.strip()
        
        # Skip until we find the data separator
        if '---' in line:
            data_started = True
            continue
            
        if not data_started or not line or not line[0].isdigit():
            continue
            
        try:
            record = parse_data_line(line, year, month, format_type)
            if record:
                records.append(record)
        except Exception as e:
            print(f"    Error parsing line in {year_month}: {line[:50]}... - {e}")
    
    return records

def parse_data_line(line, year, month, format_type):
    """Parse a single data line based on format type"""
    if len(line) < 50:
        return None
        
    # Clean up the line - handle multiple spaces
    parts = re.split(r'\s+', line.strip())
    
    if len(parts) < 10:
        return None
        
    try:
        if format_type == "format_with_deg_days":
            # Format: DAY TEMP HIGH TIME LOW TIME HEAT_DEG COOL_DEG RAIN AVG HI TIME DIR BAROM HUM
            day = parts[0]
            mean_temp = parts[1]
            high_temp = parts[2]
            high_time = parts[3]
            low_temp = parts[4]
            low_time = parts[5]
            heat_deg_days = parts[6] if len(parts) > 6 else "0"
            cool_deg_days = parts[7] if len(parts) > 7 else "0"
            rain = parts[8] if len(parts) > 8 else "0.0"
            wind_avg = parts[9] if len(parts) > 9 else "0"
            wind_hi = parts[10] if len(parts) > 10 else "0"
            wind_hi_time = parts[11] if len(parts) > 11 else ""
            dom_dir = parts[12] if len(parts) > 12 else ""
            mean_barom = parts[13] if len(parts) > 13 else ""
            mean_hum = parts[14] if len(parts) > 14 else ""
        else:
            # Standard format: DAY TEMP HIGH TIME LOW TIME RAIN AVG HI TIME DIR BAROM HUM RUN
            day = parts[0]
            mean_temp = parts[1]
            high_temp = parts[2]
            high_time = parts[3]
            low_temp = parts[4]
            low_time = parts[5]
            rain = parts[6] if len(parts) > 6 else "0.0"
            wind_avg = parts[7] if len(parts) > 7 else "0"
            wind_hi = parts[8] if len(parts) > 8 else "0"
            wind_hi_time = parts[9] if len(parts) > 9 else ""
            dom_dir = parts[10] if len(parts) > 10 else ""
            mean_barom = parts[11] if len(parts) > 11 else ""
            mean_hum = parts[12] if len(parts) > 12 else ""
            heat_deg_days = "0"
            cool_deg_days = "0"
        
        # Clean up barometric pressure - remove non-numeric prefixes
        mean_barom = re.sub(r'^[A-Z]+', '', mean_barom)
        
        return {
            'year': year,
            'month': str(int(month)),  # Remove zero-padding from month
            'day': day,
            'mean_temp': mean_temp,
            'high_temp': high_temp,
            'high_time': high_time,
            'low_temp': low_temp,
            'low_time': low_time,
            'heat_deg_days': heat_deg_days,
            'cool_deg_days': cool_deg_days,
            'rain': rain,
            'wind_avg': wind_avg,
            'wind_hi': wind_hi,
            'wind_hi_time': wind_hi_time,
            'dom_dir': dom_dir,
            'mean_barom': mean_barom,
            'mean_hum': mean_hum
        }
        
    except Exception as e:
        print(f"    Error parsing parts: {parts} - {e}")
        return None

def detect_anomalies(record):
    """Detect anomalies in a weather record"""
    anomalies = []
    
    try:
        # Temperature checks
        mean_temp = float(record['mean_temp'])
        high_temp = float(record['high_temp'])
        low_temp = float(record['low_temp'])
        
        # Chennai temperature checks (reasonable ranges)
        if mean_temp < 15 or mean_temp > 45:
            anomalies.append(f"Unusual mean temperature: {mean_temp}°C")
        
        if high_temp < low_temp:
            anomalies.append("High temp lower than low temp")
            
        if high_temp - low_temp > 25:
            anomalies.append(f"Extreme temperature range: {high_temp - low_temp:.1f}°C")
            
    except (ValueError, KeyError):
        pass
    
    try:
        # Barometric pressure check
        if record['mean_barom'] and record['mean_barom'] != '':
            barom = float(record['mean_barom'])
            if barom < 980 or barom > 1040:
                anomalies.append(f"Unusual barometric pressure: {barom} hPa")
    except (ValueError, KeyError):
        pass
    
    try:
        # Humidity check
        if record['mean_hum'] and record['mean_hum'] != '':
            humidity = float(record['mean_hum'])
            if humidity < 10 or humidity > 100:
                anomalies.append(f"Unusual humidity: {humidity}%")
    except (ValueError, KeyError):
        pass
    
    try:
        # Rain check
        if record['rain'] and record['rain'] != '':
            rain = float(record['rain'])
            if rain > 200:
                anomalies.append(f"Extreme rainfall: {rain}mm")
    except (ValueError, KeyError):
        pass
    
    return anomalies

def main():
    raw_data_dir = "/workspaces/clime/raw_weather_data"
    output_file = "/workspaces/clime/src/main/resources/scrambled_weather_data.csv"
    
    all_records = []
    file_count = 0
    error_count = 0
    
    # Process all files
    for filename in sorted(os.listdir(raw_data_dir)):
        if filename.endswith('.txt'):
            file_count += 1
            filepath = os.path.join(raw_data_dir, filename)
            print(f"Processing {filename}...")
            
            try:
                records = parse_weather_file(filepath)
                print(f"  Extracted {len(records)} records")
                all_records.extend(records)
            except Exception as e:
                error_count += 1
                print(f"  ERROR: {e}")
    
    print(f"\nProcessed {file_count} files with {error_count} errors")
    print(f"Total records extracted: {len(all_records)}")
    
    # Detect anomalies
    flagged_count = 0
    for record in all_records:
        anomalies = detect_anomalies(record)
        if anomalies:
            record['flagged'] = 'Y'
            record['anomaly_note'] = '; '.join(anomalies)
            flagged_count += 1
        else:
            record['flagged'] = 'F'
            record['anomaly_note'] = ''
    
    print(f"Flagged {flagged_count} records with anomalies")
    
    # Write to CSV with scrambling (shuffle records)
    import random
    random.seed(42)  # Consistent scrambling
    random.shuffle(all_records)
    
    # Write CSV
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    with open(output_file, 'w', newline='', encoding='utf-8') as csvfile:
        fieldnames = [
            'scrambled_id', 'year', 'month', 'day', 'mean_temp', 'high_temp', 'high_time',
            'low_temp', 'low_time', 'heat_deg_days', 'cool_deg_days', 'rain',
            'wind_avg', 'wind_hi', 'wind_hi_time', 'dom_dir', 'mean_barom',
            'mean_hum', 'flagged', 'anomaly_note'
        ]
        
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        
        for i, record in enumerate(all_records):
            record['scrambled_id'] = f"SC{(i * 17 + 42) % 100000:05d}"
            writer.writerow(record)
    
    print(f"\nData written to: {output_file}")
    print(f"Unscramble key: CLIME_WEATHER_KEY_2025")

if __name__ == "__main__":
    main()
