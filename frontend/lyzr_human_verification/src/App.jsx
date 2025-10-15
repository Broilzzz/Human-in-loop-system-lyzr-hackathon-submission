// src/App.jsx
import React from "react";
import { Routes, Route } from "react-router-dom";
import Home from "./pages/Home";
import Dashboard from "./pages/Dashboard";
import AgentDetail from "./pages/AgentDetails";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/dashboard/:employeeId/:employeeName" element={<Dashboard />} />
      <Route path="/agent/:roundId" element={<AgentDetail />} />
    </Routes>
  );
}
