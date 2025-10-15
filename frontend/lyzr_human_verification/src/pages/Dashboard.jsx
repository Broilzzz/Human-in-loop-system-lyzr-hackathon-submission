import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import api from "../api/api";
import Navbar from "../components/Navbar";

const statusColors = {
  PENDING: "bg-gray-300",
  APPROVED: "bg-green-400",
  REJECTED: "bg-red-500",
  ONLY_FEEDBACK: "bg-cyan-400",
};

const AGENTS_PER_PAGE = 5; // adjust as needed

export default function Dashboard() {
  const { employeeId, employeeName } = useParams();
  const [agents, setAgents] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const navigate = useNavigate();

  useEffect(() => {
    api
      .get(`/agent/employee/requests/${employeeId}`)
      .then((res) => setAgents(res.data))
      .catch((err) => console.error(err));
  }, [employeeId]);

  // Pagination calculations
  const totalPages = Math.ceil(agents.length / AGENTS_PER_PAGE);
  const paginatedAgents = agents.slice(
    (currentPage - 1) * AGENTS_PER_PAGE,
    currentPage * AGENTS_PER_PAGE
  );

  return (
    <div className="p-6">
      {/* Navbar */}
      <Navbar employeeId={employeeId} employeeName={employeeName} />

      <div className="grid grid-cols-1 gap-6 mt-4">
        {paginatedAgents.map((agent) => (
          <div key={agent.agentRequestId} className="flex flex-col gap-2">
            {/* Agent name */}
            <div className="w-24 h-12 rounded-md border flex items-center justify-center font-bold">
              {agent.agentName}
            </div>

           {/* Timeline of rounds */}
            <div className="flex flex-wrap items-center gap-4 relative">
              {agent.approvalRounds
                .slice() // create a shallow copy to avoid mutating state
                .sort((a, b) => a.roundNumber - b.roundNumber) // sort ascending
                .map((round, index) => (
                  <div key={round.roundId} className="flex items-center">
                    {/* Sphere */}
                    <div
                      className={`w-6 h-6 rounded-full cursor-pointer ${statusColors[round.status?.trim()?.toUpperCase()] || 'bg-gray-300'}`}
                      title={`Round ${round.roundNumber}: ${round.status}`}
                      onClick={() => navigate(`/agent/${round.roundId}`)}
                    ></div>

                    {/* Line connecting to next sphere */}
                    {index !== agent.approvalRounds.length - 1 && (
                      <div className="w-6 h-1 bg-gray-300"></div>
                    )}
                  </div>
                ))}
            </div>
          </div>
        ))}
      </div>

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="mt-6 flex justify-center items-center gap-4">
          <button
            className="px-4 py-2 bg-gray-200 rounded hover:bg-gray-300 disabled:opacity-50"
            disabled={currentPage === 1}
            onClick={() => setCurrentPage((p) => p - 1)}
          >
            Previous
          </button>
          <span>
            Page {currentPage} of {totalPages}
          </span>
          <button
            className="px-4 py-2 bg-gray-200 rounded hover:bg-gray-300 disabled:opacity-50"
            disabled={currentPage === totalPages}
            onClick={() => setCurrentPage((p) => p + 1)}
          >
            Next
          </button>
        </div>
      )}

      {/* Archive Button */}
      <div className="mt-8 text-center">
        <button className="bg-yellow-100 border border-yellow-300 px-6 py-2 rounded-md hover:bg-yellow-200">
          ARCHIVE
        </button>
      </div>
    </div>
  );
}
