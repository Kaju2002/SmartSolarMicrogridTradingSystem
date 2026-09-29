import React from "react";

function EnergyTradingCard({ energy, price, type }) {
  return (
    <div>
      <h3>Energy Trading</h3>
      <p>Energy: {energy} kWh</p>
      <p>Price: ${price}</p>
      <p>Type: {type}</p>
    </div>
  );
}

export default EnergyTradingCard;
