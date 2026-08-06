const canvas = document.getElementById('game-canvas')
const context = canvas.getContext('2d')

const gameContainer = document.getElementById(
  'game-container',
)

const scoreElement = document.getElementById(
  'score',
)

const highScoreElement = document.getElementById(
  'high-score',
)

const startOverlay = document.getElementById(
  'start-overlay',
)

const gameOverOverlay = document.getElementById(
  'game-over-overlay',
)

const startButton = document.getElementById(
  'start-button',
)

const restartButton = document.getElementById(
  'restart-button',
)

const mobileJumpButton = document.getElementById(
  'mobile-jump-button',
)

const finalScoreElement = document.getElementById(
  'final-score',
)

const GAME_WIDTH = canvas.width
const GAME_HEIGHT = canvas.height
const GROUND_Y = 310

const GRAVITY = 0.9
const JUMP_VELOCITY = -17

let animationFrameId = null
let previousTime = 0
let gameRunning = false
let score = 0
let highScore = loadHighScore()
let gameSpeed = 8
let obstacleTimer = 0
let obstacleDelay = 1200

let obstacles = []
let clouds = []
let groundMarks = []

const dinosaur = {
  x: 110,
  y: GROUND_Y - 70,
  width: 62,
  height: 70,
  velocityY: 0,
  grounded: true,
  animationFrame: 0,
}

function loadHighScore() {
  const storedScore = Number(
    localStorage.getItem('javaServerDinoHighScore'),
  )

  if (Number.isNaN(storedScore)) {
    return 0
  }

  return storedScore
}

function saveHighScore() {
  localStorage.setItem(
    'javaServerDinoHighScore',
    String(highScore),
  )
}

function formatScore(value) {
  return Math.floor(value)
    .toString()
    .padStart(5, '0')
}

function updateScoreDisplay() {
  scoreElement.textContent = formatScore(score)
  highScoreElement.textContent =
    formatScore(highScore)
}

function createClouds() {
  clouds = [
    {
      x: 180,
      y: 100,
      speed: 0.35,
      scale: 1,
    },
    {
      x: 580,
      y: 140,
      speed: 0.25,
      scale: 0.8,
    },
    {
      x: 950,
      y: 85,
      speed: 0.3,
      scale: 1.1,
    },
  ]
}

function createGroundMarks() {
  groundMarks = []

  for (let index = 0; index < 28; index += 1) {
    groundMarks.push({
      x: Math.random() * GAME_WIDTH,
      width: 4 + Math.random() * 18,
      offsetY: 10 + Math.random() * 16,
    })
  }
}

function resetGame() {
  cancelAnimationFrame(animationFrameId)

  score = 0
  gameSpeed = 8
  obstacleTimer = 0
  obstacleDelay = 1000
  previousTime = 0
  obstacles = []

  dinosaur.y = GROUND_Y - dinosaur.height
  dinosaur.velocityY = 0
  dinosaur.grounded = true
  dinosaur.animationFrame = 0

  createClouds()
  createGroundMarks()
  updateScoreDisplay()
  drawGame()
}

function startGame() {
  resetGame()

  gameRunning = true

  startOverlay.classList.add('hidden')
  gameOverOverlay.classList.add('hidden')

  gameContainer.focus()

  animationFrameId = requestAnimationFrame(
    gameLoop,
  )
}

function endGame() {
  gameRunning = false

  cancelAnimationFrame(animationFrameId)

  if (score > highScore) {
    highScore = Math.floor(score)
    saveHighScore()
  }

  updateScoreDisplay()

  finalScoreElement.textContent =
    `Score: ${formatScore(score)}`

  gameOverOverlay.classList.remove('hidden')
}

function jump() {
  if (!gameRunning) {
    return
  }

  if (!dinosaur.grounded) {
    return
  }

  dinosaur.velocityY = JUMP_VELOCITY
  dinosaur.grounded = false
}

function createObstacle() {
  const types = [
    {
      width: 28,
      height: 62,
    },
    {
      width: 38,
      height: 82,
    },
    {
      width: 54,
      height: 58,
    },
    {
      width: 70,
      height: 68,
    },
  ]

  const type = types[
    Math.floor(Math.random() * types.length)
  ]

  obstacles.push({
    x: GAME_WIDTH + 30,
    y: GROUND_Y - type.height,
    width: type.width,
    height: type.height,
  })

  const minimumDelay = Math.max(
    620,
    1050 - gameSpeed * 25,
  )

  obstacleDelay =
    minimumDelay + Math.random() * 750
}

function updateDinosaur(deltaScale) {
  dinosaur.velocityY += GRAVITY * deltaScale
  dinosaur.y += dinosaur.velocityY * deltaScale

  const groundPosition =
    GROUND_Y - dinosaur.height

  if (dinosaur.y >= groundPosition) {
    dinosaur.y = groundPosition
    dinosaur.velocityY = 0
    dinosaur.grounded = true
  }

  if (dinosaur.grounded) {
    dinosaur.animationFrame +=
      0.22 * deltaScale
  }
}

function updateObstacles(deltaScale, deltaTime) {
  obstacleTimer += deltaTime

  if (obstacleTimer >= obstacleDelay) {
    createObstacle()
    obstacleTimer = 0
  }

  obstacles.forEach((obstacle) => {
    obstacle.x -= gameSpeed * deltaScale
  })

  obstacles = obstacles.filter((obstacle) => {
    return obstacle.x + obstacle.width > -40
  })
}

function updateClouds(deltaScale) {
  clouds.forEach((cloud) => {
    cloud.x -= cloud.speed * deltaScale

    if (cloud.x > -140) {
      return
    }

    cloud.x = GAME_WIDTH + Math.random() * 300
    cloud.y = 60 + Math.random() * 100
  })
}

function updateGroundMarks(deltaScale) {
  groundMarks.forEach((mark) => {
    mark.x -= gameSpeed * deltaScale

    if (mark.x + mark.width >= 0) {
      return
    }

    mark.x = GAME_WIDTH + Math.random() * 100
    mark.width = 4 + Math.random() * 18
  })
}

function isColliding(first, second) {
  const horizontalPadding = 10
  const verticalPadding = 6

  return (
    first.x + horizontalPadding <
      second.x + second.width &&
    first.x + first.width - horizontalPadding >
      second.x &&
    first.y + verticalPadding <
      second.y + second.height &&
    first.y + first.height - verticalPadding >
      second.y
  )
}

function checkCollision() {
  return obstacles.some((obstacle) => {
    return isColliding(dinosaur, obstacle)
  })
}

function updateGame(deltaTime) {
  const deltaScale = Math.min(
    deltaTime / 16.67,
    2,
  )

  score += deltaTime * 0.012

  gameSpeed = Math.min(
    17,
    8 + score / 170,
  )

  updateDinosaur(deltaScale)
  updateObstacles(deltaScale, deltaTime)
  updateClouds(deltaScale)
  updateGroundMarks(deltaScale)

  if (checkCollision()) {
    endGame()
    return
  }

  updateScoreDisplay()
}

function drawBackground() {
  context.fillStyle = '#ffffff'

  context.fillRect(
    0,
    0,
    GAME_WIDTH,
    GAME_HEIGHT,
  )
}

function drawCloud(cloud) {
  const x = cloud.x
  const y = cloud.y
  const scale = cloud.scale

  context.strokeStyle = '#b8b8b8'
  context.lineWidth = 4
  context.lineJoin = 'miter'

  context.beginPath()

  context.moveTo(x, y + 22 * scale)
  context.lineTo(x + 14 * scale, y + 22 * scale)
  context.lineTo(x + 22 * scale, y + 12 * scale)
  context.lineTo(x + 38 * scale, y + 12 * scale)
  context.lineTo(x + 48 * scale, y + 4 * scale)
  context.lineTo(x + 66 * scale, y + 4 * scale)
  context.lineTo(x + 78 * scale, y + 12 * scale)
  context.lineTo(x + 92 * scale, y + 12 * scale)
  context.lineTo(x + 104 * scale, y + 22 * scale)
  context.lineTo(x, y + 22 * scale)

  context.stroke()
}

function drawClouds() {
  clouds.forEach(drawCloud)
}

function drawGround() {
  context.strokeStyle = '#3a3a3a'
  context.lineWidth = 3

  context.beginPath()
  context.moveTo(0, GROUND_Y)
  context.lineTo(GAME_WIDTH, GROUND_Y)
  context.stroke()

  context.strokeStyle = '#777777'
  context.lineWidth = 2

  groundMarks.forEach((mark) => {
    context.beginPath()

    context.moveTo(
      mark.x,
      GROUND_Y + mark.offsetY,
    )

    context.lineTo(
      mark.x + mark.width,
      GROUND_Y + mark.offsetY,
    )

    context.stroke()
  })
}

function drawDinosaurBody() {
  const x = dinosaur.x
  const y = dinosaur.y

  context.fillStyle = '#4b4b4b'

  context.fillRect(
    x + 20,
    y + 18,
    32,
    38,
  )

  context.fillRect(
    x + 36,
    y,
    30,
    30,
  )

  context.fillRect(
    x + 54,
    y + 8,
    18,
    14,
  )

  context.fillRect(
    x + 50,
    y + 24,
    17,
    8,
  )

  context.fillRect(
    x + 8,
    y + 36,
    24,
    16,
  )

  context.fillRect(
    x,
    y + 30,
    14,
    10,
  )

  context.fillRect(
    x + 15,
    y + 46,
    15,
    14,
  )

  context.fillStyle = '#ffffff'

  context.fillRect(
    x + 54,
    y + 7,
    5,
    5,
  )

  context.fillRect(
    x + 54,
    y + 25,
    12,
    4,
  )
}

function drawDinosaurLegs() {
  const x = dinosaur.x
  const y = dinosaur.y

  context.fillStyle = '#4b4b4b'

  if (!dinosaur.grounded) {
    context.fillRect(
      x + 22,
      y + 54,
      10,
      16,
    )

    context.fillRect(
      x + 42,
      y + 52,
      10,
      15,
    )

    return
  }

  const firstFrame =
    Math.floor(dinosaur.animationFrame) % 2 === 0

  if (firstFrame) {
    context.fillRect(
      x + 20,
      y + 54,
      10,
      16,
    )

    context.fillRect(
      x + 42,
      y + 54,
      17,
      8,
    )
  } else {
    context.fillRect(
      x + 20,
      y + 54,
      17,
      8,
    )

    context.fillRect(
      x + 44,
      y + 54,
      10,
      16,
    )
  }
}

function drawDinosaur() {
  drawDinosaurBody()
  drawDinosaurLegs()
}

function drawCactus(obstacle) {
  const {
    x,
    y,
    width,
    height,
  } = obstacle

  context.fillStyle = '#4b4b4b'

  const stemWidth = Math.max(
    13,
    width * 0.34,
  )

  const stemX =
    x + width / 2 - stemWidth / 2

  context.fillRect(
    stemX,
    y,
    stemWidth,
    height,
  )

  const leftArmY = y + height * 0.45

  context.fillRect(
    x,
    leftArmY,
    width / 2,
    10,
  )

  context.fillRect(
    x,
    leftArmY - 22,
    10,
    30,
  )

  if (width < 45) {
    return
  }

  const rightArmY = y + height * 0.62

  context.fillRect(
    stemX + stemWidth / 2,
    rightArmY,
    width / 2,
    10,
  )

  context.fillRect(
    x + width - 10,
    rightArmY - 26,
    10,
    34,
  )
}

function drawObstacles() {
  obstacles.forEach(drawCactus)
}

function drawGame() {
  drawBackground()
  drawClouds()
  drawGround()
  drawDinosaur()
  drawObstacles()
}

function gameLoop(timestamp) {
  if (!gameRunning) {
    return
  }

  if (previousTime === 0) {
    previousTime = timestamp
  }

  const deltaTime = Math.min(
    timestamp - previousTime,
    40,
  )

  previousTime = timestamp

  updateGame(deltaTime)
  drawGame()

  if (gameRunning) {
    animationFrameId = requestAnimationFrame(
      gameLoop,
    )
  }
}

function handleJumpInput() {
  if (gameRunning) {
    jump()
    return
  }

  startGame()
}

function handleKeyDown(event) {
  const validKeys = [
    'Space',
    'ArrowUp',
    'KeyW',
  ]

  if (!validKeys.includes(event.code)) {
    return
  }

  event.preventDefault()
  handleJumpInput()
}

startButton.addEventListener(
  'click',
  startGame,
)

restartButton.addEventListener(
  'click',
  startGame,
)

mobileJumpButton.addEventListener(
  'click',
  handleJumpInput,
)

gameContainer.addEventListener(
  'pointerdown',
  (event) => {
    if (event.target instanceof HTMLButtonElement) {
      return
    }

    handleJumpInput()
  },
)

document.addEventListener(
  'keydown',
  handleKeyDown,
)

highScoreElement.textContent =
  formatScore(highScore)

resetGame()