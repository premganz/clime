document.addEventListener('DOMContentLoaded', function() {
  const barChart = document.querySelector('.bar-chart');
  const barItems = document.querySelectorAll('.bar-item');
  if (!barChart || barItems.length === 0) return;
  
  // Create SVG overlay for trend line
  const svgNS = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(svgNS, 'svg');
  svg.setAttribute('class', 'trend-line-svg');
  svg.style.position = 'absolute';
  svg.style.top = '0';
  svg.style.left = '0';
  svg.style.width = '100%';
  svg.style.height = '100%';
  svg.style.pointerEvents = 'none';
  
  // Create polyline element for trend line
  const polyline = document.createElementNS(svgNS, 'polyline');
  polyline.setAttribute('fill', 'none');
  polyline.setAttribute('stroke', '#e74c3c');
  polyline.setAttribute('stroke-width', '2');
  polyline.setAttribute('stroke-dasharray', '5,5');
  
  // Calculate points for trend line
  let points = '';
  barItems.forEach((item, index) => {
    const barItemWidth = item.offsetWidth;
    const x = index * (barItemWidth + 8) + (barItemWidth / 2); // 8px is the gap
    const y = 200 - parseFloat(item.dataset.trendPoint || 0); // Invert Y axis
    points += `${x},${y} `;
  });
  
  polyline.setAttribute('points', points);
  svg.appendChild(polyline);
  barChart.appendChild(svg);
});
