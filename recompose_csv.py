#!/usr/bin/env python3
"""
Recompose the weather CSV file from raw data with consistent columns.
Ensures all records have heat_deg_days and cool_deg_days columns.
"""

import os
import re
import csv
import random
from datetime import datetime

def parse_weather_file(file_path, year, month):
    """Parse a weather data file and extract records."""
    records = []
    
    with open(file_path, 'r') as f:
        lines = f.readlines()
    
    # Find data lines (skip header and summary)
    data_lines = []
    for line in lines:
        line = line.strip()
        if line and line[0].isdigit() and len(line.split()) >= 6:
            data_lines.append(line)
    
    for line in data_lines:
        try:
            record = parse_data_line(line, year, month)
            if record:
                records.append(record)
        except Exception as e:
            print(f"Error parsing line in {file_path}: {line}")
            print(f"Error: {e}")
    
    return records

def parse_data_line(line, year, month):
    """Parse a single data line, handling both formats."""
    # Normalize whitespace
    line = re.sub(r'\s+', ' ', line.strip())
    tokens = line.split()
    
    if len(tokens) < 6:
        return None
    
    try:
        day = int(tokens[0])
        
        # Extract temperature values
        mean_temp = safe_float(tokens[1], 25.0)
        high_temp = safe_float(tokens[2], 30.0)
        low_temp = safe_float(tokens[4], 20.0)  # Skip time token
        
        # Extract times (or use defaults)
        high_time = safe_time(tokens[3], "12:00pm")
        low_time = safe_time(tokens[5], "6:00am")
        
        # Initialize heat/cool degree days
        heat_deg_days = 0
        cool_deg_days = 0
        
        # Find rain, wind, and other data
        rain = 0.0
        wind_avg = 0
        wind_hi = 0
        wind_hi_time = "12:00pm"
        dom_dir = "N"
        mean_barom = 1013.0
        mean_hum = 70
        
        # Look for heat/cool degree days if present
        if "DEG" in line or any(len(token) <= 3 and token.isdigit() and int(token) >= 30 for token in tokens[6:8]):
            # Format with HEAT DEG DAYS COOL DEG DAYS
            token_idx = 6
            if token_idx < len(tokens):
                heat_deg_days = safe_int(tokens[token_idx], 0)
                token_idx += 1
            if token_idx < len(tokens):
                cool_deg_days = safe_int(tokens[token_idx], 0)
                token_idx += 1
            # Rain should be next - look for a value <= 20 (reasonable rain limit)
            if token_idx < len(tokens):
                candidate_rain = safe_float(tokens[token_idx], 0.0)
                if candidate_rain <= 20.0:  # Reasonable rain range
                    rain = candidate_rain
                token_idx += 1
        else:
            # Standard format without heat/cool degree days
            token_idx = 6
            if token_idx < len(tokens):
                candidate_rain = safe_float(tokens[token_idx], 0.0)
                if candidate_rain <= 20.0:  # Reasonable rain range
                    rain = candidate_rain
                token_idx += 1
        
        # Extract remaining fields if available
        if token_idx < len(tokens):
            wind_avg = safe_int(tokens[token_idx], 0)
            token_idx += 1
        if token_idx < len(tokens):
            wind_hi = safe_int(tokens[token_idx], 0)
            token_idx += 1
        if token_idx < len(tokens):
            wind_hi_time = safe_time(tokens[token_idx], "12:00pm")
            token_idx += 1
        if token_idx < len(tokens):
            # Direction might be concatenated with barometric pressure
            dir_barom = tokens[token_idx]
            if any(c.isalpha() for c in dir_barom) and any(c.isdigit() for c in dir_barom):
                # Split direction and barometric pressure
                dir_match = re.search(r'([A-Z]+)', dir_barom)
                barom_match = re.search(r'(\d+\.\d+)', dir_barom)
                if dir_match:
                    dom_dir = dir_match.group(1)
                if barom_match:
                    mean_barom = float(barom_match.group(1))
            else:
                dom_dir = dir_barom if dir_barom.isalpha() else "N"
            token_idx += 1
        
        # Try to get barometric pressure if not already extracted
        if mean_barom == 1013.0:
            for i in range(token_idx, min(len(tokens), token_idx + 3)):
                if re.match(r'10\d{2}\.\d+', tokens[i]):
                    mean_barom = float(tokens[i])
                    break
        
        # Try to get humidity
        for i in range(max(0, len(tokens) - 3), len(tokens)):
            if tokens[i].isdigit() and 20 <= int(tokens[i]) <= 100:
                mean_hum = int(tokens[i])
                break
        
        return {
            'year': year,
            'month': month,
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
        print(f"Error parsing line: {line} - {e}")
        return None

def safe_float(value, default):
    """Safely convert to float."""
    try:
        return float(value)
    except (ValueError, TypeError):
        return default

def safe_int(value, default):
    """Safely convert to int."""
    try:
        return int(float(value))
    except (ValueError, TypeError):
        return default

def safe_time(value, default):
    """Safely extract time."""
    if re.match(r'\d{1,2}:\d{2}[ap]m', value):
        return value
    return default

def scramble_record(record):
    """Apply scrambling to sensitive data."""
    # Create scrambled ID
    scrambled_id = f"SC{random.randint(10000, 99999):05d}"
    
    # Add small random variations to temperatures (±1°C)
    temp_variation = random.uniform(-1.0, 1.0)
    record['mean_temp'] += temp_variation
    record['high_temp'] += temp_variation
    record['low_temp'] += temp_variation
    
    # Add small variations to other numeric fields
    record['mean_barom'] += random.uniform(-2.0, 2.0)
    record['mean_hum'] += random.randint(-5, 5)
    record['mean_hum'] = max(20, min(100, record['mean_hum']))
    
    return scrambled_id, record

def detect_anomalies(record):
    """Detect anomalies in the weather record."""
    anomalies = []
    
    # Temperature checks
    if record['mean_temp'] < 10 or record['mean_temp'] > 45:
        anomalies.append("Extreme temperature")
    
    if record['high_temp'] < record['low_temp']:
        anomalies.append("High < Low temp")
    
    if record['high_temp'] - record['low_temp'] > 25:
        anomalies.append("Large temp range")
    
    # Rain checks
    if record['rain'] > 100:
        anomalies.append("Excessive rainfall")
    
    # Pressure checks
    if record['mean_barom'] < 950 or record['mean_barom'] > 1050:
        anomalies.append("Unusual pressure")
    
    return anomalies

def main():
    raw_data_dir = "/workspaces/clime/raw_weather_data"
    output_file = "/workspaces/clime/src/main/resources/scrambled_weather_data.csv"
    
    all_records = []
    
    # Process all raw data files
    for filename in sorted(os.listdir(raw_data_dir)):
        if filename.endswith('.txt'):
            match = re.match(r'(\d{4})_(\d{2})\.txt', filename)
            if match:
                year = int(match.group(1))
                month = int(match.group(2))
                
                file_path = os.path.join(raw_data_dir, filename)
                print(f"Processing {filename}...")
                
                records = parse_weather_file(file_path, year, month)
                print(f"  Extracted {len(records)} records")
                all_records.extend(records)
    
    print(f"\nTotal records extracted: {len(all_records)}")
    
    # Write to CSV with scrambling
    with open(output_file, 'w', newline='') as csvfile:
        fieldnames = [
            'scrambled_id', 'year', 'month', 'day', 'mean_temp', 'high_temp', 
            'high_time', 'low_temp', 'low_time', 'heat_deg_days', 'cool_deg_days',
            'rain', 'wind_avg', 'wind_hi', 'wind_hi_time', 'dom_dir', 
            'mean_barom', 'mean_hum', 'flagged', 'anomaly_note'
        ]
        
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        
        for record in all_records:
            scrambled_id, scrambled_record = scramble_record(record)
            anomalies = detect_anomalies(scrambled_record)
            
            csv_record = {
                'scrambled_id': scrambled_id,
                'year': scrambled_record['year'],
                'month': scrambled_record['month'],
                'day': scrambled_record['day'],
                'mean_temp': round(scrambled_record['mean_temp'], 1),
                'high_temp': round(scrambled_record['high_temp'], 1),
                'high_time': scrambled_record['high_time'],
                'low_temp': round(scrambled_record['low_temp'], 1),
                'low_time': scrambled_record['low_time'],
                'heat_deg_days': scrambled_record['heat_deg_days'],
                'cool_deg_days': scrambled_record['cool_deg_days'],
                'rain': scrambled_record['rain'],
                'wind_avg': scrambled_record['wind_avg'],
                'wind_hi': scrambled_record['wind_hi'],
                'wind_hi_time': scrambled_record['wind_hi_time'],
                'dom_dir': scrambled_record['dom_dir'],
                'mean_barom': round(scrambled_record['mean_barom'], 2),
                'mean_hum': scrambled_record['mean_hum'],
                'flagged': 'Y' if anomalies else 'N',
                'anomaly_note': '; '.join(anomalies) if anomalies else ''
            }
            
            writer.writerow(csv_record)
    
    print(f"CSV file created: {output_file}")
    print("All records now have consistent heat_deg_days and cool_deg_days columns!")

if __name__ == "__main__":
    main()
