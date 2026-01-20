import React, { useRef, useEffect, useState, useCallback } from 'react';
import * as THREE from 'three';
import { CSS2DRenderer, CSS2DObject } from 'three/examples/jsm/renderers/CSS2DRenderer.js';
import './MirrorExpV3.css';

const slides = [
  {
    id: 'sight',
    title: 'Sight',
    description: 'is the first point of reflection.',
    details: 'It establishes the emotional tone that guides all other senses, ensuring that every Mirror experience feels composed, intentional, and emotionally legible from the first moment.'
  },
  {
    id: 'sound',
    title: 'Sound',
    description: 'Sound shapes atmosphere and emotional pacing.',
    details: 'In the Mirror ecosystem, sound is not an accessory. It is a structural element of experience, shaping how moments unfold over time.'
  },
  {
    id: 'touch',
    title: 'Touch',
    description: 'Touch confirms what the eyes believe.',
    details: 'Every texture, weight, and surface in Mirror is designed to communicate quality and care through physical interaction.'
  },
  {
    id: 'scent',
    title: 'Scent',
    description: 'Scent creates invisible architecture.',
    details: 'It defines spaces and moments, leaving lasting impressions that transcend visual memory.'
  },
  {
    id: 'taste',
    title: 'Taste',
    description: 'Taste completes the sensory journey.',
    details: 'It transforms experience into memory, creating moments that linger long after they end.'
  }
];

// Orbit configuration - 10 orbits matching V2 SVG ellipse values (scaled by 1/150)
// V2 SVG values: orbit1=204x42, orbit2=415x88, orbit3=439x88, orbit4=692x142,
// orbit5=743x142, orbit6=906x197, orbit7=964x198, orbit8=1295x266, orbit9=1415x266, orbit10=1675x292
const orbitConfig = [
  { rx: 1.36, ry: 0.28, color: 0xbc224c, opacity: 1.0, glowIntensity: 0.8 },     // orbit 1 (204/150, 42/150)
  { rx: 2.77, ry: 0.59, color: 0xbc224c, opacity: 0.95, glowIntensity: 0.7 },    // orbit 2 (415/150, 88/150)
  { rx: 2.93, ry: 0.59, color: 0xbc224c, opacity: 0.9, glowIntensity: 0.65 },    // orbit 3 (439/150, 88/150)
  { rx: 4.61, ry: 0.95, color: 0xbc224c, opacity: 0.85, glowIntensity: 0.55 },   // orbit 4 (692/150, 142/150)
  { rx: 4.95, ry: 0.95, color: 0x9040a0, opacity: 0.75, glowIntensity: 0.5 },    // orbit 5 (743/150, 142/150)
  { rx: 6.04, ry: 1.31, color: 0x7050b0, opacity: 0.65, glowIntensity: 0.45 },   // orbit 6 (906/150, 197/150)
  { rx: 6.43, ry: 1.32, color: 0x6464c8, opacity: 0.55, glowIntensity: 0.4 },    // orbit 7 (964/150, 198/150)
  { rx: 8.63, ry: 1.77, color: 0x6464c8, opacity: 0.5, glowIntensity: 0.35 },    // orbit 8 (1295/150, 266/150)
  { rx: 9.43, ry: 1.77, color: 0x5858b8, opacity: 0.45, glowIntensity: 0.3 },    // orbit 9 (1415/150, 266/150)
  { rx: 11.17, ry: 1.95, color: 0x5050a8, opacity: 0.4, glowIntensity: 0.25 },   // orbit 10 (1675/150, 292/150)
];

// Planet configuration - matching V2 design (6 planets)
const planetConfig = [
  { orbit: 9, angle: 180, icon: 'rect' },      // far left, outer orbit
  { orbit: 4, angle: 135, icon: 'circle' },    // upper left
  { orbit: 5, angle: 105, icon: 'droplet' },   // upper middle
  { orbit: 6, angle: 55, icon: 'circle' },     // upper right
  { orbit: 7, angle: 25, icon: 'diamond' },    // right upper
  { orbit: 8, angle: 310, icon: 'heart' },     // bottom right
];

// Small dots configuration - matching V2 design (8 dots)
const smallDotConfig = [
  { orbit: 2, angle: 125 },
  { orbit: 3, angle: 85 },
  { orbit: 4, angle: 45 },
  { orbit: 5, angle: 15 },
  { orbit: 6, angle: 165 },
  { orbit: 7, angle: 140 },
  { orbit: 8, angle: 0 },
  { orbit: 9, angle: 330 },
];

const MirrorExpV3 = () => {
  const containerRef = useRef(null);
  const sceneRef = useRef(null);
  const rendererRef = useRef(null);
  const labelRendererRef = useRef(null);
  const cameraRef = useRef(null);
  const orbitsRef = useRef([]);
  const planetsRef = useRef([]);
  const smallDotsRef = useRef([]);
  const centerRef = useRef(null);
  const frameRef = useRef(null);

  const [currentSlide, setCurrentSlide] = useState(0);

  const handlePrev = useCallback(() => {
    setCurrentSlide((prev) => (prev === 0 ? slides.length - 1 : prev - 1));
  }, []);

  const handleNext = useCallback(() => {
    setCurrentSlide((prev) => (prev === slides.length - 1 ? 0 : prev + 1));
  }, []);

  const currentContent = slides[currentSlide];
  const nextSlide = slides[(currentSlide + 1) % slides.length];

  // Shader for gradient orbit stroke (mimics Figma SVG gradient)
  const orbitVertexShader = `
    attribute float lineProgress;
    varying float vProgress;
    void main() {
      vProgress = lineProgress;
      gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
    }
  `;

  const orbitFragmentShader = `
    uniform vec3 colorPink;
    uniform vec3 colorWhite;
    uniform float opacity;
    uniform float time;
    varying float vProgress;

    void main() {
      // Animated gradient position (slow rotation)
      float animOffset = time * 0.05;
      float p = mod(vProgress + animOffset, 1.0);

      // V2 gradient pattern: pink(0%) → white(12%) → black(25-75%) → pink(88%) → white(100%)
      vec3 color;
      float alpha = opacity;

      if (p < 0.12) {
        // Pink to white (0% - 12%)
        float t = p / 0.12;
        color = mix(colorPink, colorWhite, t);
        alpha = opacity;
      } else if (p < 0.25) {
        // White to black (12% - 25%)
        float t = (p - 0.12) / 0.13;
        color = mix(colorWhite, vec3(0.0), t);
        alpha = opacity * (1.0 - t * 0.8);
      } else if (p < 0.75) {
        // Black/transparent (25% - 75%)
        color = vec3(0.0);
        alpha = opacity * 0.15;
      } else if (p < 0.88) {
        // Black to pink (75% - 88%)
        float t = (p - 0.75) / 0.13;
        color = mix(vec3(0.0), colorPink, t);
        alpha = opacity * (0.15 + t * 0.85);
      } else {
        // Pink to white (88% - 100%)
        float t = (p - 0.88) / 0.12;
        color = mix(colorPink, colorWhite, t);
        alpha = opacity;
      }

      gl_FragColor = vec4(color, alpha);
    }
  `;

  // Create ellipse geometry with progress attribute for shader
  const createShaderEllipseGeometry = useCallback((rx, ry, segments = 512) => {
    const points = [];
    const progress = [];

    for (let i = 0; i <= segments; i++) {
      const angle = (i / segments) * Math.PI * 2;
      points.push(
        Math.cos(angle) * rx,
        0,
        Math.sin(angle) * ry
      );
      progress.push(i / segments);
    }

    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute('position', new THREE.Float32BufferAttribute(points, 3));
    geometry.setAttribute('lineProgress', new THREE.Float32BufferAttribute(progress, 1));

    return geometry;
  }, []);

  // Initialize Three.js scene
  useEffect(() => {
    if (!containerRef.current) return;

    const container = containerRef.current;
    const width = container.clientWidth;
    const height = container.clientHeight;

    // Scene
    const scene = new THREE.Scene();
    sceneRef.current = scene;

    // Camera - perspective for depth
    const camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000);
    camera.position.set(0, 8, 12);
    camera.lookAt(0, 0, 0);
    cameraRef.current = camera;

    // WebGL Renderer
    const renderer = new THREE.WebGLRenderer({
      antialias: true,
      alpha: true,
      powerPreference: 'high-performance'
    });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setClearColor(0x000000, 0);
    container.appendChild(renderer.domElement);
    rendererRef.current = renderer;

    // CSS2D Renderer for HTML elements
    const labelRenderer = new CSS2DRenderer();
    labelRenderer.setSize(width, height);
    labelRenderer.domElement.style.position = 'absolute';
    labelRenderer.domElement.style.top = '0';
    labelRenderer.domElement.style.left = '0';
    labelRenderer.domElement.style.pointerEvents = 'none';
    container.appendChild(labelRenderer.domElement);
    labelRendererRef.current = labelRenderer;

    // Create 3D orbits with gradient shader only (clean look)
    orbitConfig.forEach((config) => {
      const orbitGroup = new THREE.Group();
      const pinkColor = new THREE.Color(config.color);

      // Main orbit line with gradient shader
      const mainGeometry = createShaderEllipseGeometry(config.rx, config.ry);
      const shaderMaterial = new THREE.ShaderMaterial({
        vertexShader: orbitVertexShader,
        fragmentShader: orbitFragmentShader,
        uniforms: {
          colorPink: { value: new THREE.Vector3(pinkColor.r, pinkColor.g, pinkColor.b) },
          colorWhite: { value: new THREE.Vector3(1.0, 1.0, 1.0) },
          opacity: { value: config.opacity },
          time: { value: 0.0 }
        },
        transparent: true,
        depthWrite: false,
        blending: THREE.AdditiveBlending,
      });
      const mainLine = new THREE.Line(mainGeometry, shaderMaterial);
      orbitGroup.add(mainLine);

      orbitGroup.rotation.x = Math.PI * 0.15; // Tilt for 3D perspective
      scene.add(orbitGroup);
      orbitsRef.current.push({ group: orbitGroup, shaderMaterial });
    });

    // Create center diamond with CSS2DObject (matching V2 SVG)
    const centerDiv = document.createElement('div');
    centerDiv.className = 'mirror-exp-v3__center';
    centerDiv.innerHTML = `
      <svg xmlns="http://www.w3.org/2000/svg" width="71" height="70" viewBox="0 0 71 70" fill="none" class="mirror-exp-v3__center-diamond">
        <g filter="url(#filter0_center_v3)">
          <ellipse cx="35.4666" cy="34.5333" rx="26.1333" ry="25.2" fill="url(#paint0_radial_center_v3)"/>
          <path d="M35.467 10.0833C49.5116 10.0835 60.8499 21.0557 60.8499 34.5334C60.8497 48.0111 49.5115 58.9834 35.467 58.9836C21.4223 58.9836 10.0834 48.0112 10.0833 34.5334C10.0833 21.0556 21.4223 10.0833 35.467 10.0833Z" stroke="url(#paint1_linear_center_v3)" stroke-width="1.5"/>
        </g>
        <g clip-path="url(#paint2_diamond_clip_v3)">
          <g transform="matrix(0 0.035 -0.0342667 0 35.4667 35)">
            <rect x="0" y="0" width="1028.57" height="1064.2" fill="url(#paint2_diamond_center_v3)" opacity="1" shape-rendering="crispEdges"/>
            <rect x="0" y="0" width="1028.57" height="1064.2" transform="scale(1 -1)" fill="url(#paint2_diamond_center_v3)" opacity="1" shape-rendering="crispEdges"/>
            <rect x="0" y="0" width="1028.57" height="1064.2" transform="scale(-1 1)" fill="url(#paint2_diamond_center_v3)" opacity="1" shape-rendering="crispEdges"/>
            <rect x="0" y="0" width="1028.57" height="1064.2" transform="scale(-1)" fill="url(#paint2_diamond_center_v3)" opacity="1" shape-rendering="crispEdges"/>
          </g>
        </g>
        <defs>
          <filter id="filter0_center_v3" x="5.33325" y="5.33325" width="60.2666" height="58.3999" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
            <feFlood flood-opacity="0" result="BackgroundImageFix"/>
            <feBlend mode="normal" in="SourceGraphic" in2="BackgroundImageFix" result="shape"/>
            <feTurbulence type="fractalNoise" baseFrequency="2 2" numOctaves="3" seed="8287"/>
            <feDisplacementMap in="shape" scale="8" xChannelSelector="R" yChannelSelector="G" result="displacedImage" width="100%" height="100%"/>
            <feMerge result="effect1_texture">
              <feMergeNode in="displacedImage"/>
            </feMerge>
          </filter>
          <clipPath id="paint2_diamond_clip_v3">
            <ellipse cx="35.4667" cy="35" rx="35.4667" ry="35"/>
          </clipPath>
          <radialGradient id="paint0_radial_center_v3" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(35.4666 34.5333) rotate(90) scale(25.2 26.1333)">
            <stop offset="0.9" stop-opacity="0"/>
            <stop offset="1" stop-color="#FDFDFB" stop-opacity="0.5"/>
          </radialGradient>
          <linearGradient id="paint1_linear_center_v3" x1="63.9823" y1="36.3229" x2="5.93972" y2="34.4915" gradientUnits="userSpaceOnUse">
            <stop stop-color="#BC224C"/>
            <stop offset="0.0961538" stop-color="white"/>
            <stop offset="0.293269"/>
            <stop offset="0.716346"/>
            <stop offset="0.875" stop-color="#BC224C"/>
            <stop offset="1" stop-color="white"/>
          </linearGradient>
          <linearGradient id="paint2_diamond_center_v3" x1="0" y1="0" x2="500" y2="500" gradientUnits="userSpaceOnUse">
            <stop offset="0.144231" stop-color="white"/>
            <stop offset="0.567308" stop-color="#1F1C1B" stop-opacity="0"/>
          </linearGradient>
        </defs>
      </svg>
    `;

    const centerLabel = new CSS2DObject(centerDiv);
    centerLabel.position.set(0, 0, 0);
    scene.add(centerLabel);
    centerRef.current = centerLabel;

    // Create planets with HTML elements (matching V2 PlanetOrb style)
    planetConfig.forEach((config, index) => {
      const orbitData = orbitConfig[config.orbit];
      const angleRad = (config.angle * Math.PI) / 180;
      const uniqueId = `planet_${index}_${Date.now()}`;

      // Create HTML element for planet with SVG orb and icon
      const planetDiv = document.createElement('div');
      planetDiv.className = 'mirror-exp-v3__planet';
      planetDiv.innerHTML = `
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="60"
          height="60"
          viewBox="0 0 94 95"
          fill="none"
          class="mirror-exp-v3__planet-orb"
        >
          <g filter="url(#filter_${uniqueId})">
            <ellipse cx="47" cy="47.5" rx="43" ry="43.5" fill="url(#radial_${uniqueId})"/>
            <path
              d="M47 4.75C70.3259 4.75 89.25 23.8816 89.25 47.5C89.25 71.1184 70.3259 90.25 47 90.25C23.6741 90.25 4.75 71.1184 4.75 47.5C4.75 23.8816 23.6741 4.75 47 4.75Z"
              stroke="url(#linear_${uniqueId})"
              stroke-width="1.5"
            />
          </g>
          <defs>
            <filter id="filter_${uniqueId}" x="0" y="0" width="94" height="95" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
              <feFlood flood-opacity="0" result="BackgroundImageFix"/>
              <feBlend mode="normal" in="SourceGraphic" in2="BackgroundImageFix" result="shape"/>
              <feTurbulence type="fractalNoise" baseFrequency="2 2" numOctaves="3" seed="8287"/>
              <feDisplacementMap in="shape" scale="8" xChannelSelector="R" yChannelSelector="G" result="displacedImage" width="100%" height="100%"/>
              <feMerge result="effect_${uniqueId}">
                <feMergeNode in="displacedImage"/>
              </feMerge>
            </filter>
            <radialGradient id="radial_${uniqueId}" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(47 47.5) rotate(90) scale(43.5 43)">
              <stop offset="0.725962" stop-opacity="0"/>
              <stop offset="1" stop-color="#FFD9E3" stop-opacity="0.5"/>
            </radialGradient>
            <linearGradient id="linear_${uniqueId}" x1="93.9199" y1="50.5893" x2="-1.59243" y2="47.7166" gradientUnits="userSpaceOnUse">
              <stop stop-color="#BC224C"/>
              <stop offset="0.0961538" stop-color="white"/>
              <stop offset="0.293269"/>
              <stop offset="0.716346"/>
              <stop offset="0.875" stop-color="#BC224C"/>
              <stop offset="1" stop-color="white"/>
            </linearGradient>
          </defs>
        </svg>
        <span class="mirror-exp-v3__icon-glow"></span>
        <span class="mirror-exp-v3__planet-icon mirror-exp-v3__planet-icon--${config.icon}"></span>
      `;
      planetDiv.style.pointerEvents = 'auto';

      // Create CSS2DObject
      const planetLabel = new CSS2DObject(planetDiv);

      // Position on ellipse
      planetLabel.position.x = Math.cos(angleRad) * orbitData.rx;
      planetLabel.position.z = Math.sin(angleRad) * orbitData.ry;
      planetLabel.position.y = 0;

      // Store initial angle for animation
      planetLabel.userData = {
        orbit: config.orbit,
        angle: config.angle,
        speed: 0.02 + (index * 0.005), // Slower speeds
        element: planetDiv
      };

      scene.add(planetLabel);
      planetsRef.current.push(planetLabel);
    });

    // Create small dots (matching V2 SmallDot style)
    smallDotConfig.forEach((config, index) => {
      const orbitData = orbitConfig[config.orbit];
      const angleRad = (config.angle * Math.PI) / 180;
      const uniqueId = `smalldot_${index}_${Date.now()}`;

      // Create HTML element for small dot with SVG
      const dotDiv = document.createElement('div');
      dotDiv.className = 'mirror-exp-v3__small-dot-wrapper';
      dotDiv.innerHTML = `
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="54"
          height="52"
          viewBox="0 0 27 25"
          fill="none"
          class="mirror-exp-v3__small-dot"
        >
          <g clip-path="url(#clip_${uniqueId})">
            <g transform="matrix(0 0.0123786 -0.0126846 0 13.1288 12.3786)">
              <rect x="0" y="0" width="1080.78" height="1113.86" fill="url(#grad_${uniqueId})" opacity="1" shape-rendering="crispEdges"/>
              <rect x="0" y="0" width="1080.78" height="1113.86" transform="scale(1 -1)" fill="url(#grad_${uniqueId})" opacity="1" shape-rendering="crispEdges"/>
              <rect x="0" y="0" width="1080.78" height="1113.86" transform="scale(-1 1)" fill="url(#grad_${uniqueId})" opacity="1" shape-rendering="crispEdges"/>
              <rect x="0" y="0" width="1080.78" height="1113.86" transform="scale(-1)" fill="url(#grad_${uniqueId})" opacity="1" shape-rendering="crispEdges"/>
            </g>
          </g>
          <defs>
            <clipPath id="clip_${uniqueId}">
              <ellipse cx="13.1288" cy="12.3786" rx="13.1288" ry="12.3786"/>
            </clipPath>
            <linearGradient id="grad_${uniqueId}" x1="0" y1="0" x2="500" y2="500" gradientUnits="userSpaceOnUse">
              <stop stop-color="white"/>
              <stop offset="0.144231" stop-color="#BC224C"/>
              <stop offset="0.567308" stop-color="#1F1C1B" stop-opacity="0"/>
            </linearGradient>
          </defs>
        </svg>
      `;

      // Create CSS2DObject
      const dotLabel = new CSS2DObject(dotDiv);

      // Position on ellipse
      dotLabel.position.x = Math.cos(angleRad) * orbitData.rx;
      dotLabel.position.z = Math.sin(angleRad) * orbitData.ry;
      dotLabel.position.y = 0;

      // Store initial angle for animation
      dotLabel.userData = {
        orbit: config.orbit,
        angle: config.angle,
        speed: 0.01 + (index * 0.003), // Very slow
      };

      scene.add(dotLabel);
      smallDotsRef.current.push(dotLabel);
    });

    // Add ambient light
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.5);
    scene.add(ambientLight);

    // Animation loop
    let time = 0;
    const animate = () => {
      frameRef.current = requestAnimationFrame(animate);
      time += 0.016; // ~60fps timing

      // Update shader uniforms for animated gradient
      orbitsRef.current.forEach((orbitData, index) => {
        // Update shader time for gradient animation
        if (orbitData.shaderMaterial) {
          orbitData.shaderMaterial.uniforms.time.value = time + index * 0.5;
        }
        // Slight rotation for dynamic effect
        if (orbitData.group) {
          orbitData.group.rotation.z = Math.sin(time * 0.1 + index * 0.2) * 0.01;
        }
      });

      // Animate planets along orbits
      planetsRef.current.forEach((planet) => {
        const { orbit, speed } = planet.userData;
        const orbitData = orbitConfig[orbit];

        planet.userData.angle += speed;
        const angleRad = (planet.userData.angle * Math.PI) / 180;

        planet.position.x = Math.cos(angleRad) * orbitData.rx;
        planet.position.z = Math.sin(angleRad) * orbitData.ry;
      });

      // Animate small dots along orbits
      smallDotsRef.current.forEach((dot) => {
        const { orbit, speed } = dot.userData;
        const orbitData = orbitConfig[orbit];

        dot.userData.angle += speed;
        const angleRad = (dot.userData.angle * Math.PI) / 180;

        dot.position.x = Math.cos(angleRad) * orbitData.rx;
        dot.position.z = Math.sin(angleRad) * orbitData.ry;
      });

      // Pulse center diamond with CSS transform
      if (centerRef.current && centerRef.current.element) {
        const scale = 1 + Math.sin(time * 2) * 0.08;
        centerRef.current.element.style.transform = `scale(${scale})`;
      }

      renderer.render(scene, camera);
      labelRenderer.render(scene, camera);
    };
    animate();

    // Handle resize
    const handleResize = () => {
      const newWidth = container.clientWidth;
      const newHeight = container.clientHeight;

      camera.aspect = newWidth / newHeight;
      camera.updateProjectionMatrix();
      renderer.setSize(newWidth, newHeight);
      labelRenderer.setSize(newWidth, newHeight);
    };
    window.addEventListener('resize', handleResize);

    // Cleanup
    return () => {
      window.removeEventListener('resize', handleResize);
      if (frameRef.current) {
        cancelAnimationFrame(frameRef.current);
      }
      renderer.dispose();
      if (container.contains(renderer.domElement)) {
        container.removeChild(renderer.domElement);
      }
      if (container.contains(labelRenderer.domElement)) {
        container.removeChild(labelRenderer.domElement);
      }
      // Clear refs
      orbitsRef.current = [];
      planetsRef.current = [];
      smallDotsRef.current = [];
    };
  }, [createShaderEllipseGeometry, orbitVertexShader, orbitFragmentShader]);

  return (
    <section className="mirror-exp-v3">
      {/* Header */}
      <div className="mirror-exp-v3__header bodytext-4--no-margin">
        AND AWAKENING LUXURY THROUGH YOUR SENSES
      </div>

      {/* Content Slider */}
      <div className="mirror-exp-v3__slider">
        {/* Current Slide */}
        <div className="mirror-exp-v3__slide mirror-exp-v3__slide--current">
          <h2 className="mirror-exp-v3__title">{currentContent.title}</h2>
          <div className="mirror-exp-v3__nav">
            <button
              className="mirror-exp-v3__nav-btn"
              onClick={handlePrev}
              aria-label="Previous slide"
            >
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="11" stroke="currentColor" strokeWidth="1"/>
                <path d="M14 8L10 12L14 16" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </button>
            <div className="mirror-exp-v3__divider"></div>
            <button
              className="mirror-exp-v3__nav-btn"
              onClick={handleNext}
              aria-label="Next slide"
            >
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="11" stroke="currentColor" strokeWidth="1"/>
                <path d="M10 8L14 12L10 16" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </button>
          </div>
          <p className="mirror-exp-v3__description bodytext-5--no-margin">
            {currentContent.description}
          </p>
          <p className="mirror-exp-v3__details bodytext-6--no-margin">
            {currentContent.details}
          </p>
        </div>

        {/* Next Slide Preview */}
        <div className="mirror-exp-v3__slide mirror-exp-v3__slide--next">
          <h2 className="mirror-exp-v3__title mirror-exp-v3__title--faded">{nextSlide.title}</h2>
          <div className="mirror-exp-v3__divider-vertical"></div>
          <p className="mirror-exp-v3__description mirror-exp-v3__description--faded bodytext-5--no-margin">
            {nextSlide.description}
          </p>
          <p className="mirror-exp-v3__details mirror-exp-v3__details--faded bodytext-6--no-margin">
            {nextSlide.details}
          </p>
        </div>
      </div>

      {/* Three.js Universe Container */}
      <div className="mirror-exp-v3__universe" ref={containerRef}></div>
    </section>
  );
};

export default MirrorExpV3;
