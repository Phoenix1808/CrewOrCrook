# 🎮 Multi-Game Android Project

Hi guys!!  

This is my yet another Android project which contains **4 separate games** in it.  
They are as follows:

- 🔵 **Connect the Dots**
- 🧠 **Memory Game**
- 🔢 **Number Sequence**
- ❓ **Trivia Quiz**

All these games are developed in order to enhance and polish my development skills as much as possible!

---

## 🚀 Recently Added Features

1. 🔐 Authentication APIs Integrated  
2. 📝 Input Field Type Handling  
3. 👁️ Password Toggle Button  
4. ⬇️ Dropdown Menu for Branch & Year Selection  

---

## 🛠️ Upcoming Tasks

1. 🔄 Complete API Integration using **MVVM Architecture**  
2. 🎨 UI Enhancement & Polishing  
3. 🏠 Room Creation / Joining / Deletion Functionality  

---

## 📈 Vision

And the learning does not stop here.  
Continuous improvement, better architecture, and scalable implementation are the goals 🚀

---

### 💻 Tech Stack (So Far)

- Kotlin
- XML Layouts
- Retrofit (Auth APIs)
- MVVM (In Progress)
- Firebase (if integrated)
- Room (Planned)

---

⭐ Always building. Always learning.

### MARCH-3

- REST Layer
- SOCKET Layer

- In the Rest one we have covered POST/room/createNew , GET/room/available, GET/room/{code}/lookup

- In the socket part we have covered Socket Auth, SocketManager.init(token) & SocketManager.connect()
- We emitted **lobby:join:room** with ACK handled + Success Stored + joinedRoomCode
- Also **lobby:player:joined**

- What's to be done now:
- 1. **game:started** we have implemented but only toast is visible not the UI transitioning same with **game:role** , **game:error** listener is implemented
- 2.  Missing part includes **game:start (emit)** , **game:started (navigate)**, **game:role (store+pass)**
  3.  Role of LobbyAcitivty will be Room Creation/LookUp/Get Available Rooms + Lobby Join + Player Wait + Host Presses Start **In short this activity is for pre-game phase** will have another activity which will handle **In-Game Phase**
