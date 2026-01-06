import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TowerGame extends JFrame {
    private static final int WINDOW_WIDTH = 400;
    private static final int WINDOW_HEIGHT = 600;
    private static final int BLOCK_HEIGHT = 30;
    private static final int INITIAL_BLOCK_WIDTH = 200;
    private static final int BASE_Y = WINDOW_HEIGHT - 50;
    private static final String HIGH_SCORE_FILE = "highscore.txt";
    
    private enum GameState {
        MENU, PLAYING, PAUSED, GAME_OVER
    }
    
    private GameState currentState = GameState.MENU;
    private GamePanel gamePanel;
    private Timer gameTimer;
    private Timer animationTimer;
    
    private int score = 0;
    private int lives = 3;
    private int highScore = 0;
    private boolean dropping = false;
    
    private Block baseBlock;
    private Block currentBlock;
    private List<Block> stackedBlocks = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();
    private List<FallingDebris> fallingDebris = new ArrayList<>();
    
    private double swingAngle = 0;
    private double swingSpeed = 0.05;
    private int swingDirection = 1;
    private int swingRange = 150;
    
    private int dropY = 0;
    
    private int shakeOffsetX = 0;
    private int shakeOffsetY = 0;
    private int shakeIntensity = 0;
    
    private int displayedScore = 0;
    private int scoreAnimationTimer = 0;
    
    private int menuSelection = 0;
    private int gameOverSelection = 0;
    
    private boolean perfectPlacement = false;
    private int perfectFlashTimer = 0;
    
    public TowerGame() {
        setTitle("都市摩天楼 - Tower Bloxx");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);
        
        loadHighScore();
        
        gamePanel = new GamePanel();
        add(gamePanel);
        
        setupControls();
        
        gameTimer = new Timer(16, e -> gameLoop());
        gameTimer.start();
        
        animationTimer = new Timer(16, e -> updateAnimations());
        animationTimer.start();
    }
    
    private void loadHighScore() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(HIGH_SCORE_FILE));
            String line = reader.readLine();
            if (line != null) {
                highScore = Integer.parseInt(line);
            }
            reader.close();
        } catch (Exception e) {
            highScore = 0;
        }
    }
    
    private void saveHighScore() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(HIGH_SCORE_FILE));
            writer.write(String.valueOf(highScore));
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupControls() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (currentState) {
                    case MENU:
                        handleMenuInput(e);
                        break;
                    case PLAYING:
                        handleGameInput(e);
                        break;
                    case PAUSED:
                        handlePauseInput(e);
                        break;
                    case GAME_OVER:
                        handleGameOverInput(e);
                        break;
                }
            }
        });
        
        setFocusable(true);
        requestFocusInWindow();
    }
    
    private void handleMenuInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
            menuSelection = (menuSelection + 1) % 2;
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (menuSelection == 0) {
                startGame();
            } else {
                System.exit(0);
            }
        }
    }
    
    private void handleGameInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (!dropping) {
                dropBlock();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_P) {
            currentState = GameState.PAUSED;
        }
    }
    
    private void handlePauseInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_P) {
            currentState = GameState.PLAYING;
        } else if (e.getKeyCode() == KeyEvent.VK_Q) {
            currentState = GameState.MENU;
        }
    }
    
    private void handleGameOverInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
            gameOverSelection = (gameOverSelection + 1) % 2;
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (gameOverSelection == 0) {
                startGame();
            } else {
                currentState = GameState.MENU;
            }
        }
    }
    
    private void startGame() {
        score = 0;
        lives = 3;
        displayedScore = 0;
        dropping = false;
        swingAngle = 0;
        swingSpeed = 0.05;
        swingDirection = 1;
        shakeIntensity = 0;
        perfectFlashTimer = 0;
        
        stackedBlocks.clear();
        particles.clear();
        fallingDebris.clear();
        
        baseBlock = new Block(WINDOW_WIDTH / 2 - INITIAL_BLOCK_WIDTH / 2, BASE_Y, INITIAL_BLOCK_WIDTH, BLOCK_HEIGHT, Color.GRAY);
        stackedBlocks.add(baseBlock);
        
        spawnCurrentBlock();
        currentState = GameState.PLAYING;
    }
    
    private void spawnCurrentBlock() {
        if (stackedBlocks.isEmpty()) {
            currentBlock = new Block(WINDOW_WIDTH / 2 - INITIAL_BLOCK_WIDTH / 2, 50, INITIAL_BLOCK_WIDTH, BLOCK_HEIGHT, Color.BLUE);
        } else {
            Block topBlock = stackedBlocks.get(stackedBlocks.size() - 1);
            currentBlock = new Block(WINDOW_WIDTH / 2 - topBlock.width / 2, 50, topBlock.width, BLOCK_HEIGHT, getRandomColor());
        }
        swingAngle = 0;
    }
    
    private Color getRandomColor() {
        Color[] colors = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN};
        return colors[score % colors.length];
    }
    
    private void dropBlock() {
        dropping = true;
        dropY = currentBlock.y;
    }
    
    private void gameLoop() {
        if (currentState == GameState.PLAYING && !dropping) {
            updateSwing();
        }
        
        if (currentState == GameState.PLAYING && dropping) {
            updateDrop();
        }
        
        updateScreenShake();
        
        gamePanel.repaint();
    }
    
    private void updateAnimations() {
        updateParticles();
        updateFallingDebris();
        updateScoreAnimation();
        updatePerfectFlash();
    }
    
    private void updateSwing() {
        swingAngle += swingSpeed * swingDirection;
        
        if (swingAngle >= 1 || swingAngle <= -1) {
            swingDirection *= -1;
        }
        
        int centerX = WINDOW_WIDTH / 2;
        currentBlock.x = (int)(centerX - currentBlock.width / 2 + swingAngle * swingRange);
    }
    
    private void updateDrop() {
        dropY += 10;
        currentBlock.y = dropY;
        
        Block topBlock = stackedBlocks.get(stackedBlocks.size() - 1);
        
        if (currentBlock.y >= topBlock.y - BLOCK_HEIGHT) {
            checkCollision(topBlock);
        }
    }
    
    private void checkCollision(Block topBlock) {
        int overlapLeft = Math.max(currentBlock.x, topBlock.x);
        int overlapRight = Math.min(currentBlock.x + currentBlock.width, topBlock.x + topBlock.width);
        int overlapWidth = overlapRight - overlapLeft;
        
        if (overlapWidth <= 0) {
            handleMiss();
        } else {
            handleHit(overlapLeft, overlapWidth, topBlock);
        }
    }
    
    private void handleMiss() {
        lives--;
        
        if (lives <= 0) {
            gameOver();
        } else {
            createFallingDebris(currentBlock);
            shakeIntensity = 10;
            spawnCurrentBlock();
            dropping = false;
        }
    }
    
    private void handleHit(int overlapLeft, int overlapWidth, Block topBlock) {
        score++;
        
        perfectPlacement = false;
        
        if (overlapWidth == currentBlock.width && overlapWidth == topBlock.width) {
            perfectPlacement = true;
            perfectFlashTimer = 30;
            createPerfectParticles(currentBlock);
        } else {
            createCutParticles(currentBlock, overlapLeft, overlapWidth, topBlock);
        }
        
        currentBlock.width = overlapWidth;
        currentBlock.x = overlapLeft;
        currentBlock.y = topBlock.y - BLOCK_HEIGHT;
        
        stackedBlocks.add(currentBlock);
        
        shiftBlocksDown();
        
        spawnCurrentBlock();
        
        swingSpeed = 0.05 + score * 0.002;
        
        dropping = false;
    }
    
    private void createPerfectParticles(Block block) {
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(
                block.x + block.width / 2,
                block.y + block.height / 2,
                Color.GREEN
            ));
        }
    }
    
    private void createCutParticles(Block currentBlock, int overlapLeft, int overlapWidth, Block topBlock) {
        int leftCut = currentBlock.x - overlapLeft;
        int rightCut = (overlapLeft + overlapWidth) - (currentBlock.x + currentBlock.width);
        
        if (leftCut > 0) {
            for (int i = 0; i < 10; i++) {
                particles.add(new Particle(
                    overlapLeft,
                    currentBlock.y + currentBlock.height / 2,
                    Color.RED
                ));
            }
        }
        
        if (rightCut < 0) {
            for (int i = 0; i < 10; i++) {
                particles.add(new Particle(
                    overlapLeft + overlapWidth,
                    currentBlock.y + currentBlock.height / 2,
                    Color.RED
                ));
            }
        }
    }
    
    private void createFallingDebris(Block block) {
        fallingDebris.add(new FallingDebris(block.x, block.y, block.width, block.height, block.color));
    }
    
    private void updateParticles() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.life <= 0) {
                particles.remove(i);
            }
        }
    }
    
    private void updateFallingDebris() {
        for (int i = fallingDebris.size() - 1; i >= 0; i--) {
            FallingDebris d = fallingDebris.get(i);
            d.update();
            if (d.y > WINDOW_HEIGHT) {
                fallingDebris.remove(i);
            }
        }
    }
    
    private void updateScoreAnimation() {
        if (displayedScore < score) {
            scoreAnimationTimer++;
            if (scoreAnimationTimer >= 3) {
                displayedScore++;
                scoreAnimationTimer = 0;
            }
        }
    }
    
    private void updatePerfectFlash() {
        if (perfectFlashTimer > 0) {
            perfectFlashTimer--;
        }
    }
    
    private void updateScreenShake() {
        if (shakeIntensity > 0) {
            Random random = new Random();
            shakeOffsetX = random.nextInt(shakeIntensity * 2) - shakeIntensity;
            shakeOffsetY = random.nextInt(shakeIntensity * 2) - shakeIntensity;
            shakeIntensity--;
        } else {
            shakeOffsetX = 0;
            shakeOffsetY = 0;
        }
    }
    
    private void shiftBlocksDown() {
        for (Block block : stackedBlocks) {
            block.y += BLOCK_HEIGHT;
        }
    }
    
    private void gameOver() {
        shakeIntensity = 20;
        currentState = GameState.GAME_OVER;
        
        if (score > highScore) {
            highScore = score;
            saveHighScore();
        }
    }
    
    private class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g.translate(shakeOffsetX, shakeOffsetY);
            
            g.setColor(Color.BLACK);
            g.fillRect(-shakeOffsetX, -shakeOffsetY, getWidth(), getHeight());
            
            switch (currentState) {
                case MENU:
                    drawMenu(g);
                    break;
                case PLAYING:
                case PAUSED:
                    drawGame(g);
                    if (currentState == GameState.PAUSED) {
                        drawPauseOverlay(g);
                    }
                    break;
                case GAME_OVER:
                    drawGame(g);
                    drawGameOver(g);
                    break;
            }
        }
        
        private void drawMenu(Graphics g) {
            g.setColor(new Color(100, 150, 255));
            g.setFont(new Font("Arial", Font.BOLD, 48));
            String title = "都市摩天楼";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(title, (WINDOW_WIDTH - fm.stringWidth(title)) / 2, 150);
            
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Tower Bloxx", (WINDOW_WIDTH - fm.stringWidth("Tower Bloxx")) / 2, 190);
            
            g.setFont(new Font("Arial", Font.BOLD, 24));
            String[] options = {"开始游戏", "退出游戏"};
            for (int i = 0; i < options.length; i++) {
                if (i == menuSelection) {
                    g.setColor(Color.YELLOW);
                    g.drawString("> " + options[i] + " <", (WINDOW_WIDTH - fm.stringWidth("> " + options[i] + " <")) / 2, 300 + i * 50);
                } else {
                    g.setColor(Color.WHITE);
                    g.drawString(options[i], (WINDOW_WIDTH - fm.stringWidth(options[i])) / 2, 300 + i * 50);
                }
            }
            
            g.setColor(Color.GRAY);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("最高分: " + highScore, (WINDOW_WIDTH - fm.stringWidth("最高分: " + highScore)) / 2, 500);
            
            g.setColor(Color.LIGHT_GRAY);
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            g.drawString("使用方向键选择，空格键确认", (WINDOW_WIDTH - fm.stringWidth("使用方向键选择，空格键确认")) / 2, 530);
        }
        
        private void drawGame(Graphics g) {
            for (Block block : stackedBlocks) {
                block.draw(g);
            }
            
            if (currentState == GameState.PLAYING) {
                currentBlock.draw(g);
            }
            
            for (FallingDebris debris : fallingDebris) {
                debris.draw(g);
            }
            
            for (Particle particle : particles) {
                particle.draw(g);
            }
            
            drawHUD(g);
        }
        
        private void drawHUD(Graphics g) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString("楼层: " + displayedScore, 20, 40);
            
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("生命: " + lives, 20, 70);
            
            g.setColor(Color.GRAY);
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            g.drawString("最高分: " + highScore, 20, 90);
            
            g.setColor(Color.LIGHT_GRAY);
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            g.drawString("P - 暂停", WINDOW_WIDTH - 80, 30);
        }
        
        private void drawPauseOverlay(Graphics g) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
            
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("暂停", (WINDOW_WIDTH - fm.stringWidth("暂停")) / 2, WINDOW_HEIGHT / 2);
            
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("按 P 或 ESC 继续", (WINDOW_WIDTH - fm.stringWidth("按 P 或 ESC 继续")) / 2, WINDOW_HEIGHT / 2 + 50);
            g.drawString("按 Q 返回主菜单", (WINDOW_WIDTH - fm.stringWidth("按 Q 返回主菜单")) / 2, WINDOW_HEIGHT / 2 + 80);
        }
        
        private void drawGameOver(Graphics g) {
            g.setColor(new Color(0, 0, 0, 200));
            g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
            
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("Game Over", (WINDOW_WIDTH - fm.stringWidth("Game Over")) / 2, WINDOW_HEIGHT / 2 - 80);
            
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 32));
            g.drawString("最终楼层: " + score, (WINDOW_WIDTH - fm.stringWidth("最终楼层: " + score)) / 2, WINDOW_HEIGHT / 2);
            
            if (score == highScore && score > 0) {
                g.setColor(Color.YELLOW);
                g.setFont(new Font("Arial", Font.BOLD, 24));
                g.drawString("新纪录!", (WINDOW_WIDTH - fm.stringWidth("新纪录!")) / 2, WINDOW_HEIGHT / 2 + 40);
            }
            
            g.setColor(Color.GRAY);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("最高分: " + highScore, (WINDOW_WIDTH - fm.stringWidth("最高分: " + highScore)) / 2, WINDOW_HEIGHT / 2 + 80);
            
            g.setFont(new Font("Arial", Font.BOLD, 24));
            String[] options = {"重新开始", "返回菜单"};
            for (int i = 0; i < options.length; i++) {
                if (i == gameOverSelection) {
                    g.setColor(Color.YELLOW);
                    g.drawString("> " + options[i] + " <", (WINDOW_WIDTH - fm.stringWidth("> " + options[i] + " <")) / 2, WINDOW_HEIGHT / 2 + 140 + i * 40);
                } else {
                    g.setColor(Color.WHITE);
                    g.drawString(options[i], (WINDOW_WIDTH - fm.stringWidth(options[i])) / 2, WINDOW_HEIGHT / 2 + 140 + i * 40);
                }
            }
        }
    }
    
    private class Block {
        int x;
        int y;
        int width;
        int height;
        Color color;
        
        Block(int x, int y, int width, int height, Color color) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
        }
        
        void draw(Graphics g) {
            drawPixelBlock(g, x, y, width, height, color);
        }
        
        private void drawPixelBlock(Graphics g, int x, int y, int width, int height, Color color) {
            g.setColor(color);
            g.fillRect(x, y, width, height);
            
            g.setColor(color.brighter());
            g.fillRect(x, y, width, 2);
            g.fillRect(x, y, 2, height);
            
            g.setColor(color.darker());
            g.fillRect(x, y + height - 2, width, 2);
            g.fillRect(x + width - 2, y, 2, height);
            
            g.setColor(new Color(color.getRed() + 30, color.getGreen() + 30, color.getBlue() + 30));
            g.fillRect(x + 4, y + 4, width - 8, height - 8);
            
            if (perfectFlashTimer > 0) {
                g.setColor(new Color(0, 255, 0, (int)(255 * (perfectFlashTimer / 30.0))));
                g.fillRect(x, y, width, height);
            }
        }
    }
    
    private class Particle {
        double x, y;
        double vx, vy;
        Color color;
        int life;
        int maxLife;
        
        Particle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            Random random = new Random();
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = random.nextDouble() * 5 + 2;
            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed;
            this.life = 30;
            this.maxLife = 30;
        }
        
        void update() {
            x += vx;
            y += vy;
            vy += 0.2;
            life--;
        }
        
        void draw(Graphics g) {
            int alpha = (int)(255 * (life / (double)maxLife));
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g.fillRect((int)x - 2, (int)y - 2, 4, 4);
        }
    }
    
    private class FallingDebris {
        int x, y, width, height;
        Color color;
        double vy;
        double rotation;
        double rotationSpeed;
        
        FallingDebris(int x, int y, int width, int height, Color color) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
            this.vy = 2;
            this.rotation = 0;
            this.rotationSpeed = 0.1;
        }
        
        void update() {
            y += vy;
            vy += 0.3;
            rotation += rotationSpeed;
        }
        
        void draw(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.rotate(rotation, x + width / 2, y + height / 2);
            g.setColor(color);
            g.fillRect(x, y, width, height);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, width, height);
            g2d.rotate(-rotation, x + width / 2, y + height / 2);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TowerGame game = new TowerGame();
            game.setVisible(true);
        });
    }
}