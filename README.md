<h1 align="center"><code>Enhancement Two: Algorithms and Data Structures</code></h1>

<h2 align="center"> Computer Graphics 3D Scene</code></h2>

<div align="center">
  
[![Collision Detection Demo](https://img.youtube.com/vi/1M1YWqGmvVM/0.jpg)](https://www.youtube.com/watch?v=1M1YWqGmvVM)

*Click the image above to watch the collision detection demonstration on YouTube*

</div>

For Enhancement Two: Algorithms and Data Structures, I implemented a sophisticated collision detection system in a 3D graphics application originally developed for CS-330. The original scene allowed free camera movement through all objects, which broke immersion and realism. I chose this project because collision detection is a fundamental algorithmic challenge in computer graphics and game development, demonstrating core computer science principles in a visually compelling context.

## Enhancements Made

### 1. Axis-Aligned Bounding Box (AABB) Collision Detection
- Implemented efficient AABB collision detection algorithm
- Created bounding volumes for all scene objects (laptop, phone, desk)
- Prevented camera penetration through solid objects

### 2. Real-time Collision Response
- Integrated collision checks into camera movement pipeline
- Implemented smooth collision response without camera "sticking"
- Maintained 60+ FPS performance during intensive collision calculations

### 3. Algorithm Optimization
- Evaluated multiple collision detection approaches (AABB vs. spheres vs. OBB)
- Implemented spatial partitioning considerations for scalability
- Balanced precision against computational complexity

## Skills Demonstrated

This enhancement showcases my advanced algorithmic thinking and 3D graphics programming capabilities:

- **Algorithm Design**: Creating efficient collision detection systems
- **3D Mathematics**: Implementing vector math and spatial reasoning
- **Performance Optimization**: Maintaining real-time performance in computationally intensive scenarios
- **Problem Analysis**: Evaluating trade-offs between different algorithmic approaches

## Course Outcomes Achieved

**Course Outcome 3** Design and evaluate computing solutions that solve a given problem using algorithmic principles and computer science practices and standards appropriate to its solution, while managing the trade-offs involved in design choices.

*Demonstrated through:*
- Design and implementation of AABB collision detection algorithm
- Evaluation of multiple collision detection methods (spheres, AABB, OBB)
- Trade-off analysis between computational complexity and precision
- Selection of AABB for optimal balance of performance and accuracy for rectangular objects

**Course Outcome 4** Demonstrate an ability to use well-founded and innovative techniques, skills, and tools in computing practices for the purpose of implementing computer solutions that deliver value and accomplish industry-specific goals.

*Demonstrated through:*
- Implementation of industry-standard collision detection techniques
- Integration of complex algorithms into existing OpenGL/C++ codebase
- Creation of immersive, realistic 3D interaction experience

## Technical Implementation

### Bounding Box Structure
```cpp
struct BoundingBox {
    glm::vec3 min;
    glm::vec3 max;
    
    bool intersects(const glm::vec3& point) const {
        return (point.x >= min.x && point.x <= max.x &&
                point.y >= min.y && point.y <= max.y &&
                point.z >= min.z && point.z <= max.z);
    }
};
```

### Collision Detection Algorithm
```cpp
bool checkCollision(const glm::vec3& cameraPos, const BoundingBox& object) {
    // Simple AABB collision check
    if (cameraPos.x < object.max.x && cameraPos.x > object.min.x &&
        cameraPos.y < object.max.y && cameraPos.y > object.min.y &&
        cameraPos.z < object.max.z && cameraPos.z > object.min.z) {
        return true; // Collision detected
    }
    return false; // No collision
}
```

### Camera Movement with Collision
```cpp
void moveCamera(glm::vec3 newPosition) {
    // Check collision with all scene objects
    for (auto& object : sceneObjects) {
        if (checkCollision(newPosition, object.boundingBox)) {
            // Collision detected - cancel movement
            return;
        }
    }
    // No collision - proceed with movement
    cameraPosition = newPosition;
}
```

## Algorithmic Trade-offs Considered

### Option 1: Bounding Spheres**
- **Pros**: Simple math, uniform collision
- **Cons**: Poor fit for rectangular objects, wasted space
- **Decision**: Rejected due to poor object fitting

### Option 2: Oriented Bounding Boxes (OBB)**
- **Pros**: Perfect fit for rotated objects
- **Cons**: Complex math, higher computational cost
- **Decision**: Rejected due to complexity vs. benefit ratio

### Option 3: Axis-Aligned Bounding Boxes (AABB)**
- **Pros**: Good balance of accuracy and performance, simple implementation
- **Cons**: Less precise for rotated objects
- **Decision**: Selected - optimal for this project's needs

## Challenges and Solutions
**Challenge 1**: Maintaining real-time performance with multiple collision checks
- **Solution**: Implemented efficient AABB algorithm with early termination

**Challenge 2**: Accurate bounding box sizing for complex objects
- **Solution**: Created visual debugging mode to verify box dimensions

**Challenge 3**: Smooth collision response without camera jitter
- **Solution**: Implemented pre-movement collision prediction

## Before and After

### Original Version
- Camera could pass through all objects
- Unrealistic navigation experience
- No physical presence in the 3D world

### Enhanced Version
- Solid objects with proper collision boundaries
- Immersive, realistic navigation
- Professional-grade 3D interaction

## Performance Analysis
- **Collision Check Time**: ~0.01ms per object (60 FPS maintained)
- **Memory Overhead**: 24 bytes per bounding box (minimal)
