import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/api"; // Axios instance

export default function Home() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

    const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email) return alert("Please enter your email");

    setLoading(true);
    try {
       const res = await api.post(`/agent/employee/email`, {
        employeeEmail: email,
        });


        const { employeeId, employeeFirstname, employeeLastName } = res.data;
        const fullName = `${employeeFirstname} ${employeeLastName}`;
        navigate(`/dashboard/${employeeId}/${encodeURIComponent(fullName)}`);
    } catch (err) {
        console.error("Error fetching employee:", err);
        alert("Employee not found or server error");
    } finally {
        setLoading(false);
    }
    };

  return (
    <div className="h-screen flex flex-col justify-center items-center bg-gray-50">
      <h1 className="text-2xl font-bold mb-6">Enter your email to continue</h1>
      <form onSubmit={handleSubmit} className="flex gap-3">
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="Enter your work email"
          className="border border-gray-400 px-4 py-2 rounded-md w-64 focus:outline-none focus:ring-2 focus:ring-blue-300"
        />
        <button
          type="submit"
          disabled={loading}
          className="bg-blue-500 text-white px-6 py-2 rounded-md hover:bg-blue-600 disabled:bg-gray-400"
        >
          {loading ? "Loading..." : "Continue"}
        </button>
      </form>
    </div>
  );
}
