import { useEffect, useRef } from "react";
import "./MetaballBackground.css";

const MetaballBackground = ({ className }) => {
  const canvasRef = useRef(null);
  const animationRef = useRef(null);
  const glRef = useRef(null);
  const shaderProgramRef = useRef(null);
  const metaballsRef = useRef([]);
  const mouseRef = useRef({ x: 0, y: 0 });
  const mouseMetaballRef = useRef(null);
  const isInitializedRef = useRef(false);

  // Shader sources
  const vertexShaderSource = `
    attribute vec4 aVertexPosition;
    void main() {
      gl_Position = aVertexPosition;
    }
  `;

  const fragmentShaderSource = `
    precision highp float;
    
    const int MAX_METABALLS = 100;
    
    uniform vec3 metaballs[MAX_METABALLS];
    uniform int metaballsAmount;
    
    uniform float spread;
    uniform float steps;
    uniform float blurStrength;
    
    uniform vec3 background;
    uniform vec3 color1;
    uniform vec3 color2;
    uniform vec3 foreground;
    
    void main() {
      vec2 position = gl_FragCoord.xy;
      
      float value = 0.0;
      
      for(int i = 0; i < MAX_METABALLS; i++) {
        vec3 currentMetaball = metaballs[i];
        
        float currentDistance = pow(max(((currentMetaball.z + spread) - distance(currentMetaball.xy, position)) / spread, 0.0), 2.0);
        
        value += currentDistance;

        if(value >= 1.0 || i > metaballsAmount - 2) break;
      }

      value = sqrt(min(value, 1.0)) * steps;

      float valueWithSteps = floor(value) / steps;
      float ramp = (1.0 - (value - floor(value)) / steps) * blurStrength;

      // 4-color gradient
      vec3 color;
      float gradientValue = valueWithSteps * ramp;
      
      if (gradientValue < 0.25) {
        // Mix between background and color1
        color = mix(background, color1, gradientValue * 4.0);
      } else if (gradientValue < 0.5) {
        // Mix between color1 and color2
        color = mix(color1, color2, (gradientValue - 0.25) * 4.0);
      } else if (gradientValue < 0.75) {
        // Mix between color2 and foreground
        color = mix(color2, foreground, (gradientValue - 0.5) * 4.0);
      } else {
        // Pure foreground or beyond
        color = foreground;
      }

      gl_FragColor = vec4(color, 1.0);
    }
  `;

  // Configuration - Exact match with original HTML file
  const CONFIGURATION = {
    spread: 500,
    steps: 500,
    blurStrength: 0.5,
    background: { r: 196, g: 27, b: 89 }, // #C41B59
    color1: { r: 96, g: 4, b: 30 }, // #60041E
    color2: { r: 29, g: 0, b: 7 }, // #1D0007
    foreground: { r: 176, g: 22, b: 78 }, // #B0164E
    speed: 100, // Use original speed to match exact behavior
  };

  // Metaball class
  class Metaball {
    constructor() {
      this.size = (100 + Math.random() * 50) * 2; // 2x the size: 200-300
      // Extended height for brand-pillars (150vh)
      const extendedHeight = window.innerHeight * 1.5;
      this.position = {
        x: Math.random() * window.innerWidth,
        y: Math.random() * extendedHeight,
      };
      this.velocity = {
        x: Math.random() * 2 - 1,
        y: Math.random() * 2 - 1,
      };
    }

    update(delta) {
      this.position.x += this.velocity.x * delta * CONFIGURATION.speed;
      this.position.y += this.velocity.y * delta * CONFIGURATION.speed;

      const radius = (this.size + CONFIGURATION.spread) / 2;
      const extendedHeight = window.innerHeight * 1.5;

      if (this.position.x < -radius) {
        this.position.x = -radius;
        this.velocity.x *= -1;
      } else if (this.position.x > window.innerWidth + radius) {
        this.position.x = window.innerWidth + radius;
        this.velocity.x *= -1;
      }

      if (this.position.y < -radius) {
        this.position.y = -radius;
        this.velocity.y *= -1;
      } else if (this.position.y > extendedHeight + radius) {
        this.position.y = extendedHeight + radius;
        this.velocity.y *= -1;
      }
    }

    toArray() {
      return [this.position.x, this.position.y, this.size];
    }
  }

  // Mouse metaball
  class MouseMetaball {
    constructor() {
      this.size = 120 * 2; // 2x the size: 240
      const extendedHeight = window.innerHeight * 1.5;
      this.position = {
        x: window.innerWidth / 2,
        y: extendedHeight / 2,
      };
      this.lerpFactor = 0.05; // Match original lerp factor
    }

    update() {
      // Smoothly interpolate towards mouse position (lerp doesn't need delta)
      this.position.x +=
        (mouseRef.current.x - this.position.x) * this.lerpFactor;
      this.position.y +=
        (mouseRef.current.y - this.position.y) * this.lerpFactor;
    }

    toArray() {
      return [this.position.x, this.position.y, this.size];
    }
  }

  // Shader compilation functions
  const loadShader = (gl, type, source) => {
    const shader = gl.createShader(type);
    gl.shaderSource(shader, source);
    gl.compileShader(shader);

    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
      console.error(
        "An error occurred compiling the shaders: " +
          gl.getShaderInfoLog(shader)
      );
      gl.deleteShader(shader);
      return null;
    }

    return shader;
  };

  const initShaderProgram = (gl, vsSource, fsSource) => {
    const vertexShader = loadShader(gl, gl.VERTEX_SHADER, vsSource);
    const fragmentShader = loadShader(gl, gl.FRAGMENT_SHADER, fsSource);

    const shaderProgram = gl.createProgram();
    gl.attachShader(shaderProgram, vertexShader);
    gl.attachShader(shaderProgram, fragmentShader);
    gl.linkProgram(shaderProgram);

    if (!gl.getProgramParameter(shaderProgram, gl.LINK_STATUS)) {
      console.error(
        "Unable to initialize the shader program: " +
          gl.getProgramInfoLog(shaderProgram)
      );
      return null;
    }

    return shaderProgram;
  };

  const initRectangleBuffer = (gl) => {
    const positionBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);

    const positions = [-1.0, 1.0, 1.0, 1.0, -1.0, -1.0, 1.0, -1.0];
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(positions), gl.STATIC_DRAW);

    return positionBuffer;
  };

  const updateCanvasSize = () => {
    const canvas = canvasRef.current;
    const gl = glRef.current;
    if (!canvas || !gl) return;

    // Use device pixel ratio for sharp rendering
    const dpr = window.devicePixelRatio || 1;
    const displayWidth = canvas.offsetWidth;
    const displayHeight = canvas.offsetHeight;

    // Skip if canvas is hidden (display: none)
    if (displayWidth === 0 || displayHeight === 0) return;

    canvas.width = displayWidth * dpr;
    canvas.height = displayHeight * dpr;

    // Scale canvas back down using CSS
    canvas.style.width = displayWidth + "px";
    canvas.style.height = displayHeight + "px";

    gl.viewport(0, 0, canvas.width, canvas.height);
  };

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    // Store references for render loop
    let shaderProgram = null;
    let rectanglePosition = null;
    let vertexPositionAttribute = null;
    let metaballsLocation = null;
    let metaballsAmountLocation = null;
    let spreadLocation = null;
    let stepsLocation = null;
    let blurStrengthLocation = null;
    let foregroundColorLocation = null;
    let color2Location = null;
    let color1Location = null;
    let backgroundColorLocation = null;

    // Initialize WebGL - can be called on mount and context restore
    const initWebGL = () => {
      const gl = canvas.getContext("webgl");
      if (!gl) {
        console.error("WebGL not supported");
        return false;
      }

      glRef.current = gl;

      // Initialize shaders and buffers
      shaderProgram = initShaderProgram(
        gl,
        vertexShaderSource,
        fragmentShaderSource
      );

      // Check if shader compilation failed
      if (!shaderProgram) {
        console.error("Failed to initialize shader program");
        return false;
      }

      rectanglePosition = initRectangleBuffer(gl);
      shaderProgramRef.current = shaderProgram;

      // Get uniform locations
      vertexPositionAttribute = gl.getAttribLocation(
        shaderProgram,
        "aVertexPosition"
      );
      metaballsLocation = gl.getUniformLocation(shaderProgram, "metaballs");
      metaballsAmountLocation = gl.getUniformLocation(
        shaderProgram,
        "metaballsAmount"
      );
      spreadLocation = gl.getUniformLocation(shaderProgram, "spread");
      stepsLocation = gl.getUniformLocation(shaderProgram, "steps");
      blurStrengthLocation = gl.getUniformLocation(
        shaderProgram,
        "blurStrength"
      );
      foregroundColorLocation = gl.getUniformLocation(
        shaderProgram,
        "foreground"
      );
      color2Location = gl.getUniformLocation(shaderProgram, "color2");
      color1Location = gl.getUniformLocation(shaderProgram, "color1");
      backgroundColorLocation = gl.getUniformLocation(
        shaderProgram,
        "background"
      );

      isInitializedRef.current = true;
      return true;
    };

    // Initialize metaballs (only once)
    const initMetaballs = () => {
      if (metaballsRef.current.length > 0) return;

      const metaballs = [];
      for (let i = 0; i < 8; i++) {
        metaballs.push(new Metaball());
      }

      const mouseMetaball = new MouseMetaball();
      metaballs.push(mouseMetaball);
      metaballsRef.current = metaballs;
      mouseMetaballRef.current = mouseMetaball;
    };

    // Try to initialize WebGL
    if (!initWebGL()) {
      return;
    }

    updateCanvasSize();
    initMetaballs();

    // Animation loop
    let lastTime = 0;
    const render = (time) => {
      const delta = (time - lastTime) / 1000;
      metaballsRef.current.forEach((mb) => mb.update(delta || 0));
      lastTime = time;

      const gl = glRef.current;

      // Skip rendering if not initialized or canvas is hidden
      if (!isInitializedRef.current || !gl || !shaderProgram) {
        animationRef.current = requestAnimationFrame(render);
        return;
      }

      // Skip rendering if canvas is hidden or has invalid size
      if (canvas.width === 0 || canvas.height === 0) {
        animationRef.current = requestAnimationFrame(render);
        return;
      }

      // Check for WebGL context loss
      if (gl.isContextLost()) {
        animationRef.current = requestAnimationFrame(render);
        return;
      }

      // Clear the canvas
      gl.clearColor(0.0, 0.0, 0.0, 1.0);
      gl.clear(gl.COLOR_BUFFER_BIT);

      // IMPORTANT: Use program BEFORE setting uniforms
      gl.useProgram(shaderProgram);

      // Set uniforms - now these will work correctly
      gl.uniform3fv(
        metaballsLocation,
        new Float32Array(metaballsRef.current.map((mb) => mb.toArray()).flat())
      );
      gl.uniform1i(metaballsAmountLocation, metaballsRef.current.length);
      gl.uniform1f(stepsLocation, CONFIGURATION.steps);
      gl.uniform1f(spreadLocation, CONFIGURATION.spread);
      gl.uniform1f(blurStrengthLocation, CONFIGURATION.blurStrength);

      gl.uniform3f(
        foregroundColorLocation,
        CONFIGURATION.foreground.r / 255,
        CONFIGURATION.foreground.g / 255,
        CONFIGURATION.foreground.b / 255
      );
      gl.uniform3f(
        color2Location,
        CONFIGURATION.color2.r / 255,
        CONFIGURATION.color2.g / 255,
        CONFIGURATION.color2.b / 255
      );
      gl.uniform3f(
        color1Location,
        CONFIGURATION.color1.r / 255,
        CONFIGURATION.color1.g / 255,
        CONFIGURATION.color1.b / 255
      );
      gl.uniform3f(
        backgroundColorLocation,
        CONFIGURATION.background.r / 255,
        CONFIGURATION.background.g / 255,
        CONFIGURATION.background.b / 255
      );

      // Set up vertex attributes
      gl.bindBuffer(gl.ARRAY_BUFFER, rectanglePosition);
      gl.vertexAttribPointer(vertexPositionAttribute, 2, gl.FLOAT, false, 0, 0);
      gl.enableVertexAttribArray(vertexPositionAttribute);

      // Draw the fullscreen quad
      gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);

      animationRef.current = requestAnimationFrame(render);
    };

    // Start animation
    animationRef.current = requestAnimationFrame(render);

    // Initialize mouse position with DPR scaling
    const dpr = window.devicePixelRatio || 1;
    const initialX = canvas.offsetWidth > 0 ? (canvas.offsetWidth / 2) * dpr : 0;
    const initialY = canvas.offsetHeight > 0 ? (canvas.offsetHeight / 2) * dpr : 0;
    mouseRef.current.x = initialX;
    mouseRef.current.y = initialY;

    // Mouse event listener that works with absolute positioning
    const handleMouseMove = (event) => {
      const rect = canvas.getBoundingClientRect();
      const dpr = window.devicePixelRatio || 1;

      // Convert to canvas internal coordinates with DPR scaling
      const x = (event.clientX - rect.left) * dpr;
      const y = (rect.height - (event.clientY - rect.top)) * dpr; // Flip Y to match WebGL coordinate system

      mouseRef.current.x = x;
      mouseRef.current.y = y;
    };

    // Add mouse event to the canvas
    canvas.addEventListener("mousemove", handleMouseMove);

    // Handle WebGL context loss and restoration
    const handleContextLost = (event) => {
      event.preventDefault();
      isInitializedRef.current = false;
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };

    const handleContextRestored = () => {
      // Reinitialize WebGL completely
      if (initWebGL()) {
        updateCanvasSize();
        animationRef.current = requestAnimationFrame(render);
      }
    };

    canvas.addEventListener("webglcontextlost", handleContextLost);
    canvas.addEventListener("webglcontextrestored", handleContextRestored);

    // Add to the document to catch all mouse movements over the canvas area
    const handleDocumentMouseMove = (event) => {
      const rect = canvas.getBoundingClientRect();
      const dpr = window.devicePixelRatio || 1;

      const x = (event.clientX - rect.left) * dpr;
      const y = (rect.height - (event.clientY - rect.top)) * dpr; // Flip Y to match WebGL coordinate system

      // Extended hover area - expand bounds by 100px on all sides
      const hoverMargin = 100;
      if (
        event.clientX - rect.left >= -hoverMargin &&
        event.clientX - rect.left <= rect.width + hoverMargin &&
        event.clientY - rect.top >= -hoverMargin &&
        event.clientY - rect.top <= rect.height + hoverMargin
      ) {
        mouseRef.current.x = Math.max(0, Math.min(canvas.width, x));
        mouseRef.current.y = Math.max(0, Math.min(canvas.height, y));
      }
    };

    document.addEventListener("mousemove", handleDocumentMouseMove);

    // Resize event listener
    const handleResize = () => {
      updateCanvasSize();
    };
    window.addEventListener("resize", handleResize);

    // ResizeObserver to detect visibility changes (mobile <-> desktop)
    let resizeObserver = null;
    if (typeof ResizeObserver !== "undefined") {
      resizeObserver = new ResizeObserver((entries) => {
        for (const entry of entries) {
          const { width, height } = entry.contentRect;
          // Canvas became visible (was hidden before)
          if (width > 0 && height > 0) {
            updateCanvasSize();
            // Reinitialize mouse position when canvas becomes visible
            const dpr = window.devicePixelRatio || 1;
            if (mouseRef.current.x === 0 && mouseRef.current.y === 0) {
              mouseRef.current.x = (width / 2) * dpr;
              mouseRef.current.y = (height / 2) * dpr;
            }
          }
        }
      });
      resizeObserver.observe(canvas);
    }

    // Cleanup
    return () => {
      isInitializedRef.current = false;
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
      canvas.removeEventListener("mousemove", handleMouseMove);
      canvas.removeEventListener("webglcontextlost", handleContextLost);
      canvas.removeEventListener("webglcontextrestored", handleContextRestored);
      document.removeEventListener("mousemove", handleDocumentMouseMove);
      window.removeEventListener("resize", handleResize);
      if (resizeObserver) {
        resizeObserver.disconnect();
      }
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      className={`metaball-background ${className || ""}`}
    />
  );
};

export default MetaballBackground;
