import React from "react";

function SolarPanelCard({ panelName, power, status }) {
  return (
    <div>
      <h3>{panelName}</h3>
      <p>Power: {power} kW</p>
      <p>Status: {status}</p>
    </div>
  );
}

export default SolarPanelCard;
