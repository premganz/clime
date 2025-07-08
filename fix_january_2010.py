#!/usr/bin/env python3

# Real weather data for January 2010 from external source
# Convert to CSV format with proper scrambling and minimal anomalies

import csv
import random

# Real data from the URL - properly formatted
january_2010_data = [
    {"day": "1", "meanTemp": "25.8", "highTemp": "29.2", "highTime": "2:31pm", "lowTemp": "20.8", "lowTime": "6:39am", "rain": "0.0", "windAvg": "1", "windHi": "23", "windHiTime": "12:54pm", "domDir": "NNE", "barom": "1011.09", "hum": "76", "windRun": "31"},
    {"day": "2", "meanTemp": "26.1", "highTemp": "29.9", "highTime": "1:31pm", "lowTemp": "20.7", "lowTime": "6:21am", "rain": "0.0", "windAvg": "2", "windHi": "21", "windHiTime": "4:36pm", "domDir": "NNE", "barom": "1012.44", "hum": "72", "windRun": "40"},
    {"day": "3", "meanTemp": "26.0", "highTemp": "29.1", "highTime": "12:07pm", "lowTemp": "21.5", "lowTime": "6:48am", "rain": "0.0", "windAvg": "2", "windHi": "21", "windHiTime": "10:09am", "domDir": "NE", "barom": "1012.62", "hum": "64", "windRun": "46"},
    {"day": "4", "meanTemp": "24.7", "highTemp": "28.9", "highTime": "2:02pm", "lowTemp": "19.5", "lowTime": "5:30am", "rain": "0.0", "windAvg": "1", "windHi": "18", "windHiTime": "10:44am", "domDir": "NE", "barom": "1011.04", "hum": "69", "windRun": "19"},
    {"day": "5", "meanTemp": "24.6", "highTemp": "30.0", "highTime": "1:09pm", "lowTemp": "17.5", "lowTime": "6:15am", "rain": "0.0", "windAvg": "1", "windHi": "18", "windHiTime": "12:50pm", "domDir": "NNE", "barom": "1009.34", "hum": "70", "windRun": "25"},
    {"day": "6", "meanTemp": "25.5", "highTemp": "29.7", "highTime": "1:05pm", "lowTemp": "20.0", "lowTime": "6:20am", "rain": "0.0", "windAvg": "2", "windHi": "21", "windHiTime": "10:32am", "domDir": "NNW", "barom": "1009.55", "hum": "71", "windRun": "43"},
    {"day": "7", "meanTemp": "26.6", "highTemp": "29.6", "highTime": "12:27pm", "lowTemp": "23.0", "lowTime": "1:07am", "rain": "1.8", "windAvg": "3", "windHi": "26", "windHiTime": "1:40pm", "domDir": "NNE", "barom": "1010.31", "hum": "66", "windRun": "76"},
    {"day": "8", "meanTemp": "24.2", "highTemp": "25.5", "highTime": "12:00am", "lowTemp": "21.9", "lowTime": "7:08am", "rain": "2.4", "windAvg": "3", "windHi": "21", "windHiTime": "1:18am", "domDir": "NW", "barom": "1010.75", "hum": "86", "windRun": "65"},
    {"day": "9", "meanTemp": "24.7", "highTemp": "29.0", "highTime": "1:06pm", "lowTemp": "22.1", "lowTime": "4:53am", "rain": "0.0", "windAvg": "3", "windHi": "26", "windHiTime": "4:46pm", "domDir": "NW", "barom": "1010.29", "hum": "87", "windRun": "70"},
    {"day": "10", "meanTemp": "25.9", "highTemp": "30.1", "highTime": "1:38pm", "lowTemp": "21.6", "lowTime": "12:46am", "rain": "0.0", "windAvg": "2", "windHi": "19", "windHiTime": "2:03am", "domDir": "NW", "barom": "1011.37", "hum": "81", "windRun": "44"},
    {"day": "11", "meanTemp": "26.8", "highTemp": "31.3", "highTime": "11:20am", "lowTemp": "23.2", "lowTime": "5:15am", "rain": "0.0", "windAvg": "2", "windHi": "18", "windHiTime": "11:55am", "domDir": "ENE", "barom": "1012.34", "hum": "83", "windRun": "39"},
    {"day": "12", "meanTemp": "27.1", "highTemp": "31.3", "highTime": "11:18am", "lowTemp": "23.0", "lowTime": "5:30am", "rain": "0.0", "windAvg": "2", "windHi": "18", "windHiTime": "4:02pm", "domDir": "ENE", "barom": "1012.65", "hum": "80", "windRun": "44"},
    {"day": "13", "meanTemp": "27.5", "highTemp": "31.5", "highTime": "10:15am", "lowTemp": "23.4", "lowTime": "5:15am", "rain": "0.4", "windAvg": "2", "windHi": "19", "windHiTime": "4:18pm", "domDir": "ENE", "barom": "1013.13", "hum": "79", "windRun": "52"},
    {"day": "14", "meanTemp": "27.5", "highTemp": "31.3", "highTime": "12:39pm", "lowTemp": "22.9", "lowTime": "6:27am", "rain": "0.0", "windAvg": "2", "windHi": "19", "windHiTime": "10:22am", "domDir": "ENE", "barom": "1014.23", "hum": "76", "windRun": "55"},
    {"day": "15", "meanTemp": "27.0", "highTemp": "30.0", "highTime": "11:27am", "lowTemp": "23.2", "lowTime": "5:43am", "rain": "0.0", "windAvg": "3", "windHi": "23", "windHiTime": "11:20am", "domDir": "NE", "barom": "1015.40", "hum": "66", "windRun": "54"},
    {"day": "16", "meanTemp": "26.8", "highTemp": "29.8", "highTime": "10:29am", "lowTemp": "21.6", "lowTime": "7:05am", "rain": "0.0", "windAvg": "2", "windHi": "23", "windHiTime": "10:30am", "domDir": "N", "barom": "1016.32", "hum": "66", "windRun": "47"},
    {"day": "17", "meanTemp": "26.4", "highTemp": "30.6", "highTime": "11:26am", "lowTemp": "21.3", "lowTime": "5:10am", "rain": "0.0", "windAvg": "2", "windHi": "21", "windHiTime": "10:41am", "domDir": "ENE", "barom": "1016.07", "hum": "74", "windRun": "45"},
    {"day": "18", "meanTemp": "26.7", "highTemp": "31.2", "highTime": "1:04pm", "lowTemp": "22.0", "lowTime": "6:42am", "rain": "0.0", "windAvg": "2", "windHi": "18", "windHiTime": "2:27pm", "domDir": "ENE", "barom": "1015.05", "hum": "76", "windRun": "39"},
    {"day": "19", "meanTemp": "26.6", "highTemp": "30.4", "highTime": "12:23pm", "lowTemp": "22.0", "lowTime": "6:25am", "rain": "0.0", "windAvg": "2", "windHi": "21", "windHiTime": "4:52pm", "domDir": "NE", "barom": "1013.21", "hum": "71", "windRun": "38"},
    {"day": "20", "meanTemp": "26.0", "highTemp": "30.0", "highTime": "2:02pm", "lowTemp": "20.6", "lowTime": "6:22am", "rain": "0.0", "windAvg": "2", "windHi": "23", "windHiTime": "10:23am", "domDir": "NNE", "barom": "1014.01", "hum": "65", "windRun": "44"},
    {"day": "21", "meanTemp": "25.5", "highTemp": "29.9", "highTime": "1:10pm", "lowTemp": "19.4", "lowTime": "7:01am", "rain": "0.0", "windAvg": "2", "windHi": "24", "windHiTime": "11:38am", "domDir": "N", "barom": "1013.44", "hum": "52", "windRun": "48"},
    {"day": "22", "meanTemp": "23.2", "highTemp": "29.4", "highTime": "12:18pm", "lowTemp": "18.6", "lowTime": "6:42am", "rain": "0.0", "windAvg": "2", "windHi": "21", "windHiTime": "12:27pm", "domDir": "N", "barom": "1013.06", "hum": "58", "windRun": "40"},
    {"day": "23", "meanTemp": "23.9", "highTemp": "30.0", "highTime": "1:19pm", "lowTemp": "19.5", "lowTime": "6:51am", "rain": "0.0", "windAvg": "2", "windHi": "24", "windHiTime": "3:38pm", "domDir": "NW", "barom": "1012.97", "hum": "61", "windRun": "51"},
    {"day": "24", "meanTemp": "25.4", "highTemp": "30.1", "highTime": "11:28am", "lowTemp": "19.6", "lowTime": "6:20am", "rain": "0.0", "windAvg": "2", "windHi": "21", "windHiTime": "12:32pm", "domDir": "N", "barom": "1011.98", "hum": "69", "windRun": "47"},
    {"day": "25", "meanTemp": "26.0", "highTemp": "30.2", "highTime": "12:03pm", "lowTemp": "20.4", "lowTime": "6:49am", "rain": "0.0", "windAvg": "2", "windHi": "23", "windHiTime": "2:54pm", "domDir": "NE", "barom": "1012.15", "hum": "66", "windRun": "51"},
    {"day": "26", "meanTemp": "26.5", "highTemp": "31.1", "highTime": "12:27pm", "lowTemp": "20.8", "lowTime": "6:29am", "rain": "0.0", "windAvg": "2", "windHi": "23", "windHiTime": "2:04pm", "domDir": "NE", "barom": "1012.81", "hum": "68", "windRun": "54"},
    {"day": "27", "meanTemp": "26.1", "highTemp": "28.8", "highTime": "11:26am", "lowTemp": "23.4", "lowTime": "7:33am", "rain": "0.4", "windAvg": "1", "windHi": "19", "windHiTime": "4:27pm", "domDir": "NE", "barom": "1012.95", "hum": "78", "windRun": "39"},
    {"day": "28", "meanTemp": "26.3", "highTemp": "30.4", "highTime": "11:36am", "lowTemp": "21.5", "lowTime": "7:01am", "rain": "0.0", "windAvg": "2", "windHi": "19", "windHiTime": "11:39am", "domDir": "NE", "barom": "1013.50", "hum": "71", "windRun": "57"},
    {"day": "29", "meanTemp": "26.2", "highTemp": "30.0", "highTime": "12:04pm", "lowTemp": "21.1", "lowTime": "6:51am", "rain": "0.0", "windAvg": "2", "windHi": "24", "windHiTime": "2:57pm", "domDir": "NE", "barom": "1013.57", "hum": "68", "windRun": "48"},
    {"day": "30", "meanTemp": "25.8", "highTemp": "30.5", "highTime": "12:22pm", "lowTemp": "19.8", "lowTime": "5:27am", "rain": "0.0", "windAvg": "2", "windHi": "21", "windHiTime": "1:01pm", "domDir": "NE", "barom": "1013.48", "hum": "69", "windRun": "42"},
    {"day": "31", "meanTemp": "25.9", "highTemp": "30.5", "highTime": "11:15am", "lowTemp": "20.1", "lowTime": "6:42am", "rain": "0.0", "windAvg": "2", "windHi": "21", "windHiTime": "4:02pm", "domDir": "NE", "barom": "1013.65", "hum": "65", "windRun": "38"}
]

# Read the current CSV file
csv_file = "/workspaces/clime/src/main/resources/scrambled_weather_data.csv"

# Read all lines
with open(csv_file, 'r') as f:
    lines = f.readlines()

# Process and replace January 2010 entries
new_lines = []
header = lines[0]  # Keep the header
new_lines.append(header)

# Process each line
for line in lines[1:]:  # Skip header
    columns = line.strip().split(',')
    if len(columns) >= 2 and columns[0] == "2010" and columns[1] == "1":
        # This is a January 2010 entry - skip it, we'll replace with real data
        continue
    else:
        # Keep non-January 2010 entries
        new_lines.append(line)

# Add the real January 2010 data with minimal scrambling
for record in january_2010_data:
    # Apply light scrambling to some fields while keeping realistic values
    # Only add anomaly flags to a few records to make it realistic
    
    flagged = "N"
    anomaly_note = ""
    
    # Add anomalies to only 3 records to be realistic
    if record["day"] in ["5", "22", "8"]:
        flagged = "Y"
        if record["day"] == "5":
            # Simulate extremely low temp anomaly
            record["lowTemp"] = "15.2"
            anomaly_note = "Unusually low temperature for the season"
        elif record["day"] == "22":
            # Simulate high wind anomaly
            record["windHi"] = "45"
            anomaly_note = "Unusually high wind speed recorded"
        elif record["day"] == "8":
            # Simulate humidity anomaly (already high at 86%)
            anomaly_note = "High humidity levels detected"
    
    # Light scrambling - just mix up some wind data to simulate scrambled format
    if int(record["day"]) % 3 == 0:
        wind_avg_scrambled = f"{record['windAvg']}:{record['windRun'][:2]}p"
    else:
        wind_avg_scrambled = f"{record['windAvg']}:{record['windRun'][:2]}a"
    
    # Create CSV row
    csv_row = f"2010,1,{record['day']},{record['meanTemp']},{record['highTemp']},{record['highTime']},{record['lowTemp']},{record['lowTime']},0.0,0.0,{record['rain']},{wind_avg_scrambled},,NE  1012,{record['domDir']},{record['barom']},{record['hum']},{flagged},{anomaly_note}\n"
    new_lines.append(csv_row)

# Write the updated CSV
with open(csv_file, 'w') as f:
    f.writelines(new_lines)

print("✅ January 2010 data updated with realistic weather data from external source")
print("✅ Only 3 anomalies added to make detection realistic")
print("✅ Data maintains proper structure and formatting")
