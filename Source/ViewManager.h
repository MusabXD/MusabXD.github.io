///////////////////////////////////////////////////////////////////////////////
// viewmanager.h
// ============
// manage the viewing of 3D objects within the viewport
//
//  AUTHOR: Brian Battersby - SNHU Instructor / Computer Science
//	Created for CS-330-Computational Graphics and Visualization, Nov. 1st, 2023
///////////////////////////////////////////////////////////////////////////////

#pragma once

#include "ShaderManager.h"
#include "camera.h"

// GLFW library
#include "GLFW/glfw3.h" 

// Forward declaration
class SceneManager;

class ViewManager
{
public:
	// constructor
	ViewManager(
		ShaderManager* pShaderManager,
		SceneManager* pSceneManager);
	// destructor
	~ViewManager();

	// mouse position callback for mouse interaction with the 3D scene
	static void Mouse_Position_Callback(GLFWwindow* window, double xMousePos, double yMousePos);

	static void UMouseScrollCallback(GLFWwindow* window, double xoffset, double yoffset);

private:
	// pointer to shader and scene manager objects
	ShaderManager* m_pShaderManager;
	SceneManager* m_pSceneManager;
	// active OpenGL display window
	GLFWwindow* m_pWindow;

	// process keyboard events for interaction with the 3D scene
	void ProcessKeyboardEvents();

	// collision detection functions
	bool CheckCameraCollision(const glm::vec3& newPosition);
	glm::vec3 ResolveCollision(const glm::vec3& newPosition, const glm::vec3& oldPosition);

	bool m_bOrthographicProjection;

	glm::mat4 GetPerspectiveProjection() const;
	glm::mat4 GetOrthographicProjection() const;

public:
	// create the initial OpenGL display window
	GLFWwindow* CreateDisplayWindow(const char* windowTitle);

	// prepare the conversion from 3D object display to 2D scene display
	void PrepareSceneView();

	void ToggleProjectionMode(bool orthographic);
};