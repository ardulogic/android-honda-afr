const int bufferSize = 128;  // Define the buffer size
char buffer[bufferSize];    // Create a buffer to store incoming data
int bufferIndex = 0;        // Index to keep track of the buffer position

void setup() {
    // Initialize the serial communication with the computer
    Serial.begin(9600);

    // Initialize the hardware serial communication with the first device
    Serial1.begin(9600);

    // Example: Sending data to the first device
    Serial1.print("GETHW\n");
    Serial.println("Ready.");
}

void loop() {
    // Read from the first device and store to buffer
    while (Serial1.available() > 0 && bufferIndex < bufferSize - 1) {
        char incomingByte1 = Serial1.read();
        buffer[bufferIndex++] = incomingByte1;
    }

    // Null-terminate the buffer and print it to the Serial monitor if data was received
    if (bufferIndex > 0) {
        buffer[bufferIndex] = '\0';  // Null-terminate the string
        Serial.print(buffer);        // Print the complete buffer
        bufferIndex = 0;             // Reset the buffer index for the next message
    }

    // Read from the computer and send to the first device
    if (Serial.available() > 0) {
        char incomingByte = Serial.read();
        Serial1.print(incomingByte);  // Send the byte to the first device
        delay(10);
    }
}
