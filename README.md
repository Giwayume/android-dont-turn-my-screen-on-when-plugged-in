# Don't Turn My Screen On When Plugged In

This app is for Android 8+.

I use my phone without a lock screen, and set the screen timeout to 10+ minutes so it's not constantly turning off. I manually press the power button when I know I am done with my phone, I don't want the operating system to do anything for me.

The biggest problem with the way I prefer to use my phone, is most distributions of Android turn the screen on when you either plug in or unplug it from a power charger (or wireless charging). Samsung One UI has the brilliance to have a separate charge notification screen display when you plug it in to power, and turns the screen off seconds later, regardless of your screen timeout setting. But MOST VERSIONS OF ANDROID JUST TURN ON THE SCREEN AND LEAVE IT ON.

This app creates a simple foreground service that monitors the power plug state and keeps the screen off when it was already off. The end result should be:
- If your screen was already off, and you plug / unplug the power, the screen should remain off!
- If your screen was already on, and you plug / unplug the power, the screen should remain on!
- Basically, the screen should be unaffected by charging!

In reality, Google doesn't provide APIs that allow a simple non-system app to do this in a clean way. So the screen will flash on quickly then back off when plugged in to power. But this is much better than the screen staying on for however long you set your screen timeout for.