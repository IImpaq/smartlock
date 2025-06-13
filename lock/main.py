import sys
import tkinter as tk
from PIL import Image, ImageTk
import asyncio

from verifier.protocol import Protocol

def display_fullscreen_qr(qr_img):
  root = tk.Tk()
  root.title("Smartlock")
  root.attributes('-fullscreen', True)

  def close_window(event=None):
    root.destroy()
    sys.exit(0)

  root.bind("<Escape>", close_window)

  root.update_idletasks()
  max_size = min(root.winfo_width() or 800, root.winfo_height() or 600)

  qr_img_resized = qr_img.resize((max_size, max_size), Image.Resampling.LANCZOS)
  tk_photo_image = ImageTk.PhotoImage(qr_img_resized)

  image_label = tk.Label(root, image=tk_photo_image)
  image_label.image = tk_photo_image # keep reference to avoid issues
  image_label.pack(expand=True)

  return root


async def main():
  # Initialize ping-pong proximity-based authentication protocol instance
  protocol = Protocol(display_name="PingPongVerifier1234")

  # Generate pairing QR code
  qr = protocol.get_pairing_code()

  root = display_fullscreen_qr(qr)

  try:
    # Start protocol
    await protocol.start()

    # Keep running and repeatedly idle for 1 second
    while True:
      root.update()
      await asyncio.sleep(1)
  finally:
    # Stop protocol
    await protocol.stop()


if __name__ == "__main__":
  asyncio.run(main())
