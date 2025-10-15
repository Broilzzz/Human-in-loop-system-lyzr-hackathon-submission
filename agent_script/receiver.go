package main

import (
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
)

func receiveHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
		return
	}

	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "Failed to read body", http.StatusInternalServerError)
		return
	}
	defer r.Body.Close()

	fmt.Println("📥 Received:", string(body))
	w.Write([]byte("Data received successfully"))
}

func main() {
	http.HandleFunc("/receive", receiveHandler)
	fmt.Println("🚀 Receiver running at http://localhost:5001/receive")
	log.Fatal(http.ListenAndServe(":5001", nil))
}
