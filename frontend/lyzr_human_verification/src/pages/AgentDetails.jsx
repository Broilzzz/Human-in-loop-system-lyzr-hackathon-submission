import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios";
import Navbar from "../components/Navbar";

export default function AgentDetail() {
  const { roundId } = useParams(); // Changed from 'id' to 'roundId'
  const navigate = useNavigate();
  const [round, setRound] = useState(null);
  const [action, setAction] = useState("");
  const [feedback, setFeedback] = useState("");

  useEffect(() => {
    console.log("Fetching round with ID:", roundId);
    if (!roundId) {
      console.error("No ID provided!");
      return;
    }
    
    axios
      .get(`http://localhost:8080/api/agent/rounds/${roundId}`)
      .then((res) => {
        console.log("API Response:", res.data);
        console.log("Status:", res.data.status);
        setRound(res.data);
      })
      .catch((err) => console.error("Error fetching round:", err));
  }, [roundId]);

  const isPending = round?.status === "PENDING";
  
  console.log("Round:", round);
  console.log("isPending:", isPending);

  const handleAction = (selectedAction) => {
    setAction(selectedAction);
  };

  const handleSend = () => {
    if (!action) return alert("Select an action first");
    if (action === "FEEDBACK" && !feedback) return alert("Feedback required");

    const status = action === "FEEDBACK" ? "ONLY_FEEDBACK" : action;

    axios
      .post(`http://localhost:8080/api/agent/employee/rounds`, {
        status,
        approvalRoundId: roundId, // <-- use roundId from URL
        feedback: feedback || null,
      })
      .then(() => {
        alert("Action sent successfully");
        navigate(-1);
      })
      .catch((err) => console.error("Action send failed:", err));
  };


  const handleDownloadAttachments = async () => {
    if (!round?.s3Links || round.s3Links.length === 0) {
      alert("No attachments found");
      return;
    }

    for (const fileUrl of round.s3Links) {
      const fileName = fileUrl.split("/").pop();
      const link = document.createElement("a");
      link.href = fileUrl;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  };

  if (!round) return <div className="text-center mt-10">Loading...</div>;

  return (
    <div className="flex flex-col items-center w-full min-h-screen bg-gray-50 p-8">
      {/* Navbar - Get employeeId and employeeName from somewhere, or pass dummy values */}
      <Navbar employeeId="employee-id" employeeName={round.employeeName} />
      
      <div className="w-full max-w-4xl bg-white shadow-md rounded-lg p-8 border border-gray-200 relative mt-6">
        {/* Header */}
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-lg font-semibold">Human Verification Dashboard</h1>
          <h2 className="text-sm font-medium text-gray-600">Welcome {"{Username}"}</h2>
        </div>

        <hr className="mb-4" />

        {/* Back Button */}
        <button
          className="absolute top-6 left-6 bg-gray-300 px-4 py-1 rounded hover:bg-gray-400"
          onClick={() => navigate(-1)}
        >
          BACK
        </button>

        {/* Agent Info */}
        <div className="flex items-center mb-6">
          <div className="w-16 h-16 rounded-full border flex items-center justify-center mr-4">
            LOGO
          </div>
          <span className="font-semibold text-lg">{round.agentName}</span>
        </div>

        {/* Updated At */}
        {round.updatedAt && (
          <div className="mb-2">
            <strong>Updated At:</strong> <span>{round.updatedAt}</span>
          </div>
        )}

        {/* Subject */}
        <div className="mb-2">
          <strong>Subject:</strong> <span>{round.subject || "N/A"}</span>
        </div>

        {/* Context */}
        <div className="mb-4">
          <strong>Context:</strong>
          <p className="mt-1 text-gray-700 leading-relaxed">
            {round.context || "No context provided"}
          </p>
        </div>

        {/* Download Attachments */}
        <button
          className="bg-yellow-200 px-4 py-2 rounded hover:bg-yellow-300 mb-6"
          onClick={handleDownloadAttachments}
        >
          Download Attachments
        </button>

        {/* Action Buttons - Only show if status is PENDING */}
        {isPending && (
          <>
            <div className="flex items-center gap-4 mb-4">
              <button
                className={`px-6 py-2 rounded ${action === "APPROVED" ? "bg-green-400" : "bg-green-200"}`}
                onClick={() => handleAction("APPROVED")}
              >
                Approve
              </button>

              <button
                className={`px-6 py-2 rounded ${action === "ONLY_FEEDBACK" ? "bg-blue-400" : "bg-blue-200"}`}
                onClick={() => handleAction("ONLY_FEEDBACK")}
              >
                Only Feedback
              </button>

              <button
                className={`px-6 py-2 rounded ${action === "REJECTED" ? "bg-red-400" : "bg-red-200"}`}
                onClick={() => handleAction("REJECTED")}
              >
                Reject
              </button>
            </div>


            {/* Feedback box */}
            {action === "FEEDBACK" && (
              <textarea
                className="w-full border-2 border-gray-400 p-3 rounded-md mb-6 min-h-32"
                placeholder="Enter feedback..."
                rows="4"
                value={feedback}
                onChange={(e) => setFeedback(e.target.value)}
              />
            )}

            {/* Send button */}
            <div className="flex justify-end">
              <button
                className="px-6 py-2 bg-blue-800 text-white rounded hover:bg-blue-900"
                onClick={handleSend}
              >
                SEND
              </button>
            </div>
          </>
        )}

        {/* Status message for non-pending rounds */}
        {!isPending && (
          <div className="mt-6 p-4 bg-gray-100 border border-gray-300 rounded">
            <p className="text-gray-700">
              <strong>Status:</strong> {round.status}
            </p>
            <p className="text-sm text-gray-600 mt-2">
              This approval round has already been processed and cannot be modified.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}