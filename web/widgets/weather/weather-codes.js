// WMO weather interpretation codes, as returned by Open-Meteo in `weather_code`.
const GROUPS = [
  [[0], 'Clear', '☀️'],
  [[1], 'Mostly clear', '🌤️'],
  [[2], 'Partly cloudy', '⛅'],
  [[3], 'Cloudy', '☁️'],
  [[45, 48], 'Fog', '🌫️'],
  [[51, 53, 55, 56, 57], 'Drizzle', '🌦️'],
  [[61, 63, 65], 'Rain', '🌧️'],
  [[66, 67], 'Freezing rain', '🌧️'],
  [[71, 73, 75, 77], 'Snow', '❄️'],
  [[80, 81, 82], 'Showers', '🌦️'],
  [[85, 86], 'Snow showers', '🌨️'],
  [[95, 96, 99], 'Thunderstorm', '⛈️'],
];

export function describeCode(code) {
  for (const [codes, text, icon] of GROUPS) if (codes.includes(code)) return { text, icon };
  return { text: 'Unknown', icon: '·' };
}
