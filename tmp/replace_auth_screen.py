import os

target_path = "app/src/main/java/com/example/ui/screens/PointlyBentoScreen.kt"
new_content_path = "/tmp/new_auth_screen.txt"

with open(target_path, "r", encoding="utf-8") as f:
    target_data = f.read()

with open(new_content_path, "r", encoding="utf-8") as f:
    new_content = f.read()

# Locate the segment start and end
start_marker = "enum class AuthScreenMode {"
end_marker = "fun PointlyVerificationScreen("

start_idx = target_data.find(start_marker)
end_idx = target_data.find(end_marker)

if start_idx == -1:
    print("Error: start_marker not found!")
    exit(1)
if end_idx == -1:
    print("Error: end_marker not found!")
    exit(1)

# Ensure start is before end
if start_idx >= end_idx:
    print("Error: start_marker is after end_marker!")
    exit(1)

# Reconstruct the file content
head = target_data[:start_idx]
tail = target_data[end_idx:]

replaced_data = head + new_content + "\n\n@Composable\n" + tail

with open(target_path, "w", encoding="utf-8") as f:
    f.write(replaced_data)

print("Replacement successful!")
