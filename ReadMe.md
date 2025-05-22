# MusicalQuiz

## Features and Screenshots

### Search Screen  
![Search Screen](screenshots/search_screen.png)  
The **Search** screen allows users to discover music by querying the Deezer API for tracks or albums. A search bar and button let you enter keywords, with options to filter results by tracks or albums using radio buttons. Results are displayed in an adaptive grid of cards showing album art, track/title information. Tapping on a result opens the details for that track or album.

### Details Screen  
![Details Screen](screenshots/details_screen.png)  
The **Details** screen displays information about a selected track or album. For a track, it shows the cover art, track title, artist name, and duration. A **Play/Pause** button lets you listen to a 30-second preview of the song. There’s also an **Add to Playlist** option.
For a track, it also shows the album art, album title, and artist name as well as list of tracks in the album and a button allowing to add all tracks of the album to a playlist.

### Playlist Screen  
![Playlist Screen](screenshots/playlist_screen.png)  
The details of a playlist shows the playlist name, description, and a list of tracks in the playlist and allows to either delete or preview a track.
Clicking on a track allows to open it's details screen.
### Quiz Screen  
![Quiz Screen](screenshots/quiz_screen.png)  
The **Quiz** screen lets you generate and play music quizzes based on your playlists. It shows a list of quizzes, allows you to create new ones, and supports two game modes: **Multiple Choice** and **Fill in the Blanks**. Each question plays a preview audio, and users answer accordingly.

---

## Architecture and Implementation

The app uses a **single-activity, multi-fragment** structure and follows the **MVVM** architecture:

- **UI Layer**: MainActivity hosts multiple Fragments managed by a BottomNavigationView.
- **ViewModels**: Each screen has a corresponding ViewModel managing UI state and business logic.
- **Repository/Service**: ViewModels fetch data from the Deezer API or the local Room database.
- **Room Database**: Used to store playlists, quizzes, and track data.
- **MediaPlayer**: Handles preview audio playback.

Navigation is handled using Android Jetpack's Navigation Component with Safe Args. RecyclerViews are used extensively for lists (tracks, playlists, quizzes).

### Architecture Diagram  
![Architecture Diagram](diagrams/architecture.png)  

To create the diagram:
- Use [draw.io](https://draw.io) or [Lucidchart](https://lucidchart.com)
- Include components like:
  - Fragments: Search, Playlist, Quiz
  - ViewModels
  - Repository (API + DB access)
  - Data sources: Deezer API, Room DB
- Show directional data flow with arrows.

---

## Unimplemented Features and Technical Issues

- **UI inconsistencies**: Some screens need visual refinement.
- **No cache system**: All data is fetched fresh from the Deezer API each time.
- **Performance**: Updates through LiveData and coroutines still take a while to update the UI sometimes.
- **Minor bugs**: Small UI glitches 

---

## Contributions

### WALTZING Loïc

### KARDAVA Elene
