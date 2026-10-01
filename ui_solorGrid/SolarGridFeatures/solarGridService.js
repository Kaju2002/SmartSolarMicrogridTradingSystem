const API_BASE_URL = "http://localhost:5000/api";

export async function getSolarPanels() {
  const response = await fetch(`${API_BASE_URL}/solar-panels`);

  if (!response.ok) {
    throw new Error("Failed to fetch solar panels");
  }

  return response.json();
}

export async function getEnergyTradingData() {
  const response = await fetch(`${API_BASE_URL}/energy-trading`);

  if (!response.ok) {
    throw new Error("Failed to fetch energy trading data");
  }

  return response.json();
}
