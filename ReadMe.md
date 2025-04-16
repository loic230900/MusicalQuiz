# MusicalQuiz Application


## Structure
```
musicalquiz/
├── README.md                         # Project documentation and report
├── app/
   ├── manifests/                    # Android manifest file
   ├── kotlin+java/
   │   └── com.example.musicalquiz/
   │       ├── MainActivity.kt       # Single host activity for fragments
   │       ├── adapter/              # RecyclerView adapters
   │       │   └── TrackAdapter.kt
   │       ├── database/             # Local database (Room)
   │       │   ├── dao/
   │       │   │   ├── PlaylistDao.kt
   │       │   │   └── QuizDao.kt
   │       │   ├── entities/
   │       │   │   ├── Playlist.kt
   │       │   │   └── Quiz.kt
   │       │   └── AppDatabase.kt
   │       ├── model/                # Data models
   │       │   ├── DeezerSearchResponse.kt
   │       │   ├── Track.kt
   │       │   ├── Album.kt
   │       │   └── Artist.kt
   │       ├── network/              # Retrofit setup
   │       │   ├── RetrofitInstance.kt
   │       │   └── DeezerApiInterface.kt
   │       ├── view/                 # UI components (Fragments)
   │       │   └── fragments/
   │       │       ├── SearchFragment.kt
   │       │       ├── DetailsFragment.kt
   │       │       ├── PlaylistFragment.kt
   │       │       └── QuizFragment.kt
   │       └── viewmodel/            # ViewModels
   │           ├── TracksViewModel.kt
   │           └── PlaylistViewModel.kt
   └── res/                          # Resources (layouts, drawables, strings)
       ├── drawable/
       ├── layout/
       ├── values/
       └── mipmap/

```
## Application Architecture

**Architecture**: MVVM (Model-View-ViewModel)

- **Model**:
    - Retrofit & Deezer API integration
    - Room Database for persistence
- **View**:
    - Single Activity (`MainActivity`) with multiple fragments.
    - Navigation using BottomNavigationView.
- **ViewModel**:
    - Data persistence across configuration changes.
    - Interaction logic between View and Model.

## Main Components and Technologies

### Retrofit (API Integration)

- Deezer API for music search and track previews.
- Asynchronous requests with coroutines.

### RecyclerView

- Display search results and playlists.
- Grid-based layout for aesthetic UI.

### Glide

- Efficiently loading album cover images from URLs.

### Room Database

- Persist user-generated playlists and quizzes locally.
- Entities: `Playlist`, `Quiz`.

### MVVM Architecture

- Clear separation of concerns between Model, View, and ViewModel.
- Robustness against configuration changes.

---

## Step-by-Step Implementation Checklist ✅

### Step 1 — Project Initialization

- [ ] Create new Android Project (Empty Views Activity, Kotlin, API level 29)
- [ ] Add dependencies: Retrofit, Room, Glide, ViewModel, LiveData, RecyclerView, Navigation

### Step 2 — Model Layer (Data Classes & Retrofit)

- [ ] Implement data classes (`Track`, `Album`, `Artist`, `DeezerSearchResponse`)
- [ ] Setup Retrofit instance (`RetrofitInstance.kt`)
- [ ] Define Deezer API interface (`DeezerApiInterface.kt`)

### Step 3 — ViewModel Layer

- [ ] Create `TracksViewModel.kt` (for search results)
- [ ] Create `PlaylistViewModel.kt` (for managing playlists/quizzes)
- [ ] Use LiveData to handle data persistence across UI changes

### Step 4 — View Layer & Fragments

- [ ] Create fragments (`SearchFragment`, `DetailsFragment`, `PlaylistFragment`, `QuizFragment`)
- [ ] Implement Navigation with BottomNavigationView
- [ ] Setup fragment transitions

### Step 5 — RecyclerView & Adapter

- [ ] Design layout (`track_item.xml`) for RecyclerView items
- [ ] Implement `TrackAdapter` to bind data from Deezer API
- [ ] Integrate Glide to load album images

### Step 6 — Database (Room)

- [ ] Define entities (`Playlist.kt`, `Quiz.kt`)
- [ ] Implement DAO interfaces (`PlaylistDao.kt`, `QuizDao.kt`)
- [ ] Configure Room database (`AppDatabase.kt`)

### Step 7 — Playlist and Quiz Management

- [ ] Implement creation and modification of playlists
- [ ] Implement quiz logic, associating quizzes with playlists
- [ ] Integrate audio previews from Deezer API into quizzes

### Step 8 — UI/UX Refinement

- [ ] Apply consistent Material Design principles
- [ ] Add appropriate error handling and user feedback

### Step 9 — Testing & Debugging

- [ ] Test app thoroughly on emulator and real devices
- [ ] Ensure robustness against screen orientation and configuration changes
- [ ] Verify all CRUD operations with Room DB

### Step 10 — Finalization

- [ ] Clean, document, and comment all code thoroughly
- [ ] Finalize README.md with screenshots and detailed explanations

---