import React from "react";
import SolarPanelCard from "./SolarPanelCard";
import EnergyTradingCard from "./EnergyTradingCard";

function Dashboard() {
  return (
    <div>
      <h1>Solar Grid Dashboard</h1>

      <SolarPanelCard
        panelName="Solar Panel 01"
        power={5.2}
        status="Active"
      />

      <EnergyTradingCard
        energy={12.5}
        price={4.25}
        type="Selling"
      />
    </div>
  );
}

export default Dashboard;
