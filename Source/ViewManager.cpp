///////////////////////////////////////////////////////////////////////////////
// viewmanager.h
// ============
// manage the viewing of 3D objects within the viewport
//
//  AUTHOR: Brian Battersby - SNHU Instructor / Computer Science
//	Created for CS-330-Computational Graphics and Visualization, Nov. 1st, 2023
///////////////////////////////////////////////////////////////////////////////

#include "ViewManager.h"

// GLM Math Header inclusions
#include <glm/glm.hpp>
#include <glm/gtx/transform.hpp>
#include <glm/gtc/type_ptr.hpp>    

// Add these includes for collision detection
#include <vector>
#include <algorithm>
#include "SceneManager.h"

// declaration of the global variables and defines
namespace
{
	// Variables for window width and height
	const int WINDOW_WIDTH = 960;
	const int WINDOW_HEIGHT = 1080;
	const char* g_ViewName = "view";
	const char* g_ProjectionName = "projection";

	// camera object used for viewing and interacting with
	// the 3D scene
	Camera* g_pCamera = nullptr;

	// these variables are used for mouse movement processing
	float gLastX = WINDOW_WIDTH / 2.0f;
	float gLastY = WINDOW_HEIGHT / 2.0f;
	bool gFirstMouse = true;

	// time between current frame and last frame
	float gDeltaTime = 0.0f;
	float gLastFrame = 0.0f;

	// the following variable is false when orthographic projection
	// is off and true when it is on
	bool bOrthographicProjection = false;

	// pointer to scene manager for collision detection
	SceneManager* g_SceneManager = nullptr;
}

/***********************************************************
 *  ViewManager()
 *
 *  The constructor for the class
 ***********************************************************/
ViewManager::ViewManager(
	ShaderManager* pShaderManager,
	SceneManager* pSceneManager)
{
	// initialize the member variables
	m_pShaderManager = pShaderManager;
	m_pSceneManager = pSceneManager;
	m_pWindow = NULL;
	g_pCamera = new Camera();
	// default camera view parameters
	g_pCamera->Position = glm::vec3(0.0f, 5.0f, 12.0f);
	g_pCamera->Front = glm::vec3(0.0f, -0.5f, -2.0f);
	g_pCamera->Up = glm::vec3(0.0f, 1.0f, 0.0f);
	g_pCamera->Zoom = 80;

	m_bOrthographicProjection = false;
}

/***********************************************************
 *  ~ViewManager()
 *
 *  The destructor for the class
 ***********************************************************/
ViewManager::~ViewManager()
{
	// free up allocated memory
	m_pShaderManager = NULL;
	m_pWindow = NULL;
	if (NULL != g_pCamera)
	{
		delete g_pCamera;
		g_pCamera = NULL;
	}
}

/***********************************************************
 *  CheckCameraCollision()
 *
 *  This method checks if the camera would collide with any scene objects
 ***********************************************************/
bool ViewManager::CheckCameraCollision(const glm::vec3& newPosition)
{
	if (m_pSceneManager == nullptr) return false;  // Use member variable

	const auto& collisionBoxes = m_pSceneManager->GetCollisionBoxes();
	float cameraRadius = 0.5f;

	for (const auto& box : collisionBoxes) {
		// Find closest point on AABB to sphere center
		glm::vec3 closestPoint;
		closestPoint.x = std::max(box.min.x, std::min(newPosition.x, box.max.x));
		closestPoint.y = std::max(box.min.y, std::min(newPosition.y, box.max.y));
		closestPoint.z = std::max(box.min.z, std::min(newPosition.z, box.max.z));

		// Check if closest point is within sphere radius
		float distance = glm::length(newPosition - closestPoint);
		if (distance < cameraRadius) {
			return true;
		}
	}
	return false;
}


/***********************************************************
 *  ResolveCollision()
 *
 *  This method tries to resolve collisions by sliding along axes
 ***********************************************************/
glm::vec3 ViewManager::ResolveCollision(const glm::vec3& newPosition, const glm::vec3& oldPosition)
{
	if (!CheckCameraCollision(newPosition)) {
		return newPosition;
	}

	// Try sliding along X axis
	glm::vec3 slideX = glm::vec3(newPosition.x, oldPosition.y, oldPosition.z);
	if (!CheckCameraCollision(slideX)) {
		return slideX;
	}

	// Try sliding along Z axis
	glm::vec3 slideZ = glm::vec3(oldPosition.x, oldPosition.y, newPosition.z);
	if (!CheckCameraCollision(slideZ)) {
		return slideZ;
	}

	// If all else fails, return old position
	return oldPosition;
}

/***********************************************************
 *  ProcessKeyboardEvents()
 *
 *  This method is called to process any keyboard events
 *  that may be waiting in the event queue.
 ***********************************************************/
void ViewManager::ProcessKeyboardEvents()
{
	// close the window if the escape key has been pressed
	if (glfwGetKey(m_pWindow, GLFW_KEY_ESCAPE) == GLFW_PRESS)
	{
		glfwSetWindowShouldClose(m_pWindow, true);
	}

	// if the camera object is null, then exit this method
	if (NULL == g_pCamera)
	{
		return;
	}

	// Store original position for collision recovery
	glm::vec3 originalPosition = g_pCamera->Position;
	bool moved = false;

	// process camera zooming in and out
	if (glfwGetKey(m_pWindow, GLFW_KEY_W) == GLFW_PRESS)
	{
		g_pCamera->ProcessKeyboard(FORWARD, gDeltaTime);
		moved = true;
	}
	if (glfwGetKey(m_pWindow, GLFW_KEY_S) == GLFW_PRESS)
	{
		g_pCamera->ProcessKeyboard(BACKWARD, gDeltaTime);
		moved = true;
	}

	// process camera panning left and right
	if (glfwGetKey(m_pWindow, GLFW_KEY_A) == GLFW_PRESS)
	{
		g_pCamera->ProcessKeyboard(LEFT, gDeltaTime);
		moved = true;
	}
	if (glfwGetKey(m_pWindow, GLFW_KEY_D) == GLFW_PRESS)
	{
		g_pCamera->ProcessKeyboard(RIGHT, gDeltaTime);
		moved = true;
	}

	// process camera panning up and down
	if (glfwGetKey(m_pWindow, GLFW_KEY_Q) == GLFW_PRESS)
	{
		g_pCamera->ProcessKeyboard(DOWN, gDeltaTime);
		moved = true;
	}
	if (glfwGetKey(m_pWindow, GLFW_KEY_E) == GLFW_PRESS)
	{
		g_pCamera->ProcessKeyboard(UP, gDeltaTime);
		moved = true;
	}

	// Check for collisions and resolve if needed
	if (moved && CheckCameraCollision(g_pCamera->Position)) {
		g_pCamera->Position = ResolveCollision(g_pCamera->Position, originalPosition);
	}

	// Toggle projection mode
	if (glfwGetKey(m_pWindow, GLFW_KEY_P) == GLFW_PRESS)
	{
		ToggleProjectionMode(false);
	}
	if (glfwGetKey(m_pWindow, GLFW_KEY_O) == GLFW_PRESS)
	{
		ToggleProjectionMode(true);
	}
}


/***********************************************************
 *  CreateDisplayWindow()
 *
 *  This method is used to create the main display window.
 ***********************************************************/
GLFWwindow* ViewManager::CreateDisplayWindow(const char* windowTitle)
{
	GLFWwindow* window = nullptr;

	// try to create the displayed OpenGL window
	window = glfwCreateWindow(
		WINDOW_WIDTH,
		WINDOW_HEIGHT,
		windowTitle,
		NULL, NULL);
	if (window == NULL)
	{
		std::cout << "Failed to create GLFW window" << std::endl;
		glfwTerminate();
		return NULL;
	}
	glfwMakeContextCurrent(window);

	// Register mouse position callback
	glfwSetCursorPosCallback(window, &ViewManager::Mouse_Position_Callback);

	// Register mouse scroll callback for adjusting movement speed
	glfwSetScrollCallback(window, UMouseScrollCallback);

	// this callback is used to receive mouse moving events
	glfwSetCursorPosCallback(window, &ViewManager::Mouse_Position_Callback);

	// tell GLFW to capture all mouse events
	glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

	// enable blending for supporting tranparent rendering
	glEnable(GL_BLEND);
	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

	m_pWindow = window;

	return(window);
}

/***********************************************************
 *  Mouse_Position_Callback()
 *
 *  This method is automatically called from GLFW whenever
 *  the mouse is moved within the active GLFW display window.
 ***********************************************************/
void ViewManager::Mouse_Position_Callback(GLFWwindow* window, double xMousePos, double yMousePos)
{
	// when the first mouse move event is received, this needs to be recorded so that
	// all subsequent mouse moves can correctly calculate the X position offset and Y
	// position offset for proper operation
	if (gFirstMouse)
	{
		gLastX = xMousePos;
		gLastY = yMousePos;
		gFirstMouse = false;
	}

	// calculate the X offset and Y offset values for moving the 3D camera accordingly
	float xOffset = xMousePos - gLastX;
	float yOffset = gLastY - yMousePos; // reversed since y-coordinates go from bottom to top

	// set the current positions into the last position variables
	gLastX = xMousePos;
	gLastY = yMousePos;

	// move the 3D camera according to the calculated offsets
	g_pCamera->ProcessMouseMovement(xOffset, yOffset);
}

// Callback function for mouse scroll events
void ViewManager::UMouseScrollCallback(GLFWwindow* window, double xoffset, double yoffset)
{
	// Adjust the camera's movement speed based on the scroll input
	g_pCamera->ProcessMouseScroll(static_cast<float>(yoffset));
}


/***********************************************************
 *  PrepareSceneView()
 *
 *  This method is used for preparing the 3D scene by loading
 *  the shapes, textures in memory to support the 3D scene 
 *  rendering
 ***********************************************************/
void ViewManager::PrepareSceneView()
{
	glm::mat4 view;
	glm::mat4 projection;

	// per-frame timing
	float currentFrame = glfwGetTime();
	gDeltaTime = currentFrame - gLastFrame;
	gLastFrame = currentFrame;

	// process any keyboard events that may be waiting in the 
	// event queue
	ProcessKeyboardEvents();

	// get the current view matrix from the camera
	view = g_pCamera->GetViewMatrix();

	// Define the current projection matrix based on the projection mode
	if (m_bOrthographicProjection)
	{
		projection = GetOrthographicProjection();
	}
	else
	{
		projection = GetPerspectiveProjection();
	}

	// if the shader manager object is valid
	if (NULL != m_pShaderManager)
	{
		// set the view matrix into the shader for proper rendering
		m_pShaderManager->setMat4Value(g_ViewName, view);
		// set the view matrix into the shader for proper rendering
		m_pShaderManager->setMat4Value(g_ProjectionName, projection);
		// set the view position of the camera into the shader for proper rendering
		m_pShaderManager->setVec3Value("viewPosition", g_pCamera->Position);
	}
}

/***********************************************************
 *  ToggleProjectionMode()
 *
 *  This method is used for toggling perspectives between
 *  2D and 3D views
 ***********************************************************/
void ViewManager::ToggleProjectionMode(bool orthographic)
{
	m_bOrthographicProjection = orthographic;
	// Adjust camera for orthographic view
	if (orthographic)
	{
		g_pCamera->Position = glm::vec3(0.0f, 0.0f, 10.0f);
		g_pCamera->Front = glm::vec3(0.0f, 0.0f, -1.0f);
		g_pCamera->Up = glm::vec3(0.0f, 1.0f, 0.0f);
	}
	else
	{
		// Reset to default perspective camera settings
		g_pCamera->Position = glm::vec3(0.0f, 5.0f, 12.0f);
		g_pCamera->Front = glm::vec3(0.0f, -0.5f, -2.0f);
		g_pCamera->Up = glm::vec3(0.0f, 1.0f, 0.0f);
	}
}

glm::mat4 ViewManager::GetPerspectiveProjection() const
{
	return glm::perspective(glm::radians(g_pCamera->Zoom),
		(GLfloat)WINDOW_WIDTH / (GLfloat)WINDOW_HEIGHT,
		0.1f, 1000.0f);
}

glm::mat4 ViewManager::GetOrthographicProjection() const
{
	float aspect = (float)WINDOW_WIDTH / (float)WINDOW_HEIGHT;
	float orthoSize = 10.0f;
	return glm::ortho(-orthoSize * aspect, orthoSize * aspect,
		-orthoSize, orthoSize,
		0.1f, 1000.0f);
}
