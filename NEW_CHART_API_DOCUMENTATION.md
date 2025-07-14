# New Chart API Endpoints Documentation

This document describes the two new charting features added to the Chennai Rainfall API.

## 1. Year-wise Bar Chart

### Endpoint
```
GET /api/rainfallv2/charts/yearly-bar
```

### Parameters
- `dataSource` (optional, default: "CSV"): Data source to use ("CSV" or "KWS")

### Description
Creates a bar chart displaying rainfall data year by year. This chart shows:
- Individual bars for each year of data
- Color-coded bars based on rainfall amount:
  - **Green**: High rainfall (top 20% of range)
  - **Orange**: Medium-high rainfall (60-80% of range)
  - **Blue**: Medium rainfall (30-60% of range)
  - **Red**: Low rainfall (bottom 30% of range)
- Year labels on x-axis (every 5th year for readability)
- Scrollable horizontal view for long datasets
- Summary statistics below the chart

### Example Usage
```bash
curl "http://localhost:8080/api/rainfallv2/charts/yearly-bar"
curl "http://localhost:8080/api/rainfallv2/charts/yearly-bar?dataSource=KWS"
```

### Response
Returns HTML content with an SVG bar chart and summary statistics.

## 2. Monthly Offset Yearly Chart

### Endpoint
```
GET /api/rainfallv2/charts/yearly-offset
```

### Parameters
- `monthOffset` (required): Month to start the fiscal year (1-12, where 1=Jan, 2=Feb, etc.)
- `dataSource` (optional, default: "CSV"): Data source to use ("CSV" or "KWS")

### Description
Creates a line chart showing yearly rainfall totals calculated with a monthly offset. This allows viewing data in fiscal years that don't start in January. For example:
- `monthOffset=4` shows rainfall from April to March of the following year
- `monthOffset=7` shows rainfall from July to June of the following year
- `monthOffset=1` shows regular calendar year (January to December)

The chart displays:
- Line graph with data points
- Purple color scheme to distinguish from regular yearly charts
- Markers every 5 years for clarity
- Summary statistics for the offset period

### Example Usage
```bash
# Indian fiscal year (April to March)
curl "http://localhost:8080/api/rainfallv2/charts/yearly-offset?monthOffset=4"

# Monsoon year (July to June)
curl "http://localhost:8080/api/rainfallv2/charts/yearly-offset?monthOffset=7"

# Calendar year (January to December)
curl "http://localhost:8080/api/rainfallv2/charts/yearly-offset?monthOffset=1"
```

### Validation
- `monthOffset` must be between 1 and 12
- Invalid values return an error message

### Response
Returns HTML content with an SVG line chart and summary statistics for the offset period.

## Implementation Details

### Code Changes Made
1. **RainfallAnalyticsService.java**: Added two new methods:
   - `generateYearlyRainfallBarChartHtml()`: Creates the year-wise bar chart
   - `generateOffsetYearlyChartHtml(int monthOffset)`: Creates the offset yearly chart

2. **RainfallControllerV2.java**: Added two new endpoints:
   - `/charts/yearly-bar`: Exposes the bar chart functionality
   - `/charts/yearly-offset`: Exposes the offset chart functionality

3. **ChartFunctionalityTest.java**: Added comprehensive tests for both features

### Design Principles
- **Minimal Changes**: Reused existing architecture and patterns
- **Consistency**: Both endpoints follow the same parameter and response patterns as existing charts
- **Error Handling**: Proper validation and error messages for invalid inputs
- **Color Coding**: Intuitive visual representation of data ranges
- **Responsive Design**: Charts work well in different screen sizes

### Data Sources Supported
Both new chart types work with:
- **CSV Data**: Historical rainfall data (1901-2021)
- **KWS Data**: More recent data from KWS Chennai website (2000-2025)

### Related Existing Endpoints
These new endpoints complement the existing chart functionality:
- `/api/rainfallv2/charts/annual`: Yearly line chart
- `/api/rainfallv2/charts/decade`: Decade comparison chart
- `/api/rainfallv2/charts/decade-offset`: Decade chart with year offset
- `/api/rainfallv2/charts/monthly`: Monthly average chart
- `/api/rainfallv2/charts/monthly-trend`: Monthly trend chart

## Testing

The implementation includes comprehensive tests in `ChartFunctionalityTest.java`:
- Tests for successful chart generation with both data sources
- Tests for invalid month offset validation
- Tests for edge cases (months 1 and 12)
- Integration with existing test suite

All tests pass and maintain compatibility with existing functionality.