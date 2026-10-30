# Ophelia

A one-deck solitaire card game, as taught by Grandma.

## How to play

- Ace counts 1, number cards count their face value, Jack 11, Queen 12, King 13.
- Turn over one card at a time. A King is 13 on its own and goes straight to the pile.
- If the card makes 13 with, or matches, the top or bottom card of the line, both go to the pile.
- Otherwise the card is laid at the bottom of the line. Cards are never laid at the top.
- Whenever the top and bottom cards, or the two cards side by side at either end, make 13 or a pair, they go to the pile too. This can chain.
- You may also add three or more cards to make 13: your card plus cards taken off the top and bottom ends of the line, with no gaps. It's optional, and it leaves other cards without partners, so it usually makes winning harder.
- You win if the line is empty when the deck runs out.

When a match is waiting, the phone buzzes twice and the cards glow green. When only a 13 made from three or more cards is possible, the phone gives one long buzz and the cards glow blue. The game plays sideways or upright.

When a game ends, the results screen lists any matches you missed along the way, and separately any optional 13s you passed up.

Under the line, a **card count** chart shows every card: still in the deck, in the line, in your hand, or on the pile, with pile and left counts for each suit and a count of what's left of each rank.

Turn the phone upright and the table rearranges: the line fills the middle of the screen and shows many more cards at once, with your card and the buttons at the bottom.

The ⚙ button has these settings: **Real deck** turns off every buzz, glow and running total so you spot matches yourself; **Classic rules** allows only pairs and two-card 13s; **Card count** shows or hides the chart; **Screen** chooses whether the table turns with the phone or stays sideways or upright.

## Getting the app

Every push to `main` builds the app on GitHub. This link always downloads the newest build:

**https://github.com/randaladams/Ophelia/releases/latest/download/Ophelia.apk**

Open it on your Android phone, then open the downloaded file to install. Android will ask you to allow installing apps from your browser or file manager the first time.

## Project layout

- `app/src/main/assets/index.html` holds the whole game: the rules, the table and the cards.
- `app/src/main/java/com/randaladams/ophelia/MainActivity.java` shows the game full screen in landscape and handles the vibration.
- `.github/workflows/build.yml` builds the APK and publishes it as a release.
