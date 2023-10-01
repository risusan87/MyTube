# MyTube Discord Bot
MyTube is a simple Discord bot that allows users to stream YouTube as audio in their voice channels.
This project and bot is not meant to be publicly released. (At this time) By any chance someone wanted MyTube in a server, go ahead and clone this repository.

### How to stream an youtube audio:</br>
1. Type `/join` to let the bot join in a vc you are in. The channels must be visible for the bot.
2. `/addsong [youtube_url]` to add songs. YouTube playlists also work and recommended. Make sure they are not private in order for the bot to recognize.

Functionality for audio player actions such as skipping and shuffling is currently under experimentation and these are the only commands supported:</br>
`/skip` to skip the current music playing.</br>
`/playpause` to toggle Play/Pause in the track.</br>

**This bot is not officially endorsed or affiliated with YouTube in any way, and using it to stream copyrighted content may violate YouTube's terms of service.**

# Development
MyTube is written in Java8. All dependencies are included as a maven project. Make sure to have maven installed on your machine.
add `mytube.secret` file under `src/main/resources`

