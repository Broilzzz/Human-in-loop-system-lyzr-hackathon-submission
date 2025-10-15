import React from "react";
import { useNavigate } from "react-router-dom";
import LOGO from "../assets/logo.png";

export default function Navbar({ employeeId, employeeName }) {
  const navigate = useNavigate();

  return (
    <div className="flex items-center justify-between mb-6 border-b pb-3">
      {/* Logo on the left */}
      <img
        src={LOGO}
        alt="Logo"
        className="w-32 h-auto cursor-pointer"
        onClick={() =>
          navigate(`/dashboard/${employeeId}/${encodeURIComponent(employeeName)}`)
        }
      />

      {/* Center title */}
      <h1 className="text-2xl font-bold text-center flex-1">
        Human Verification Dashboard
      </h1>

      {/* Right welcome text */}
      <span className="ml-4">Welcome {decodeURIComponent(employeeName)}</span>
    </div>
  );
}
