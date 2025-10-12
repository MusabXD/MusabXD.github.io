///////////////////////////////////////////////////////////////////////////////
// shadermanager.cpp
// ============
// manage the loading and rendering of 3D scenes
//
//  AUTHOR: Brian Battersby - SNHU Instructor / Computer Science
//	Created for CS-330-Computational Graphics and Visualization, Nov. 1st, 2023
///////////////////////////////////////////////////////////////////////////////

#include "SceneManager.h"

#ifndef STB_IMAGE_IMPLEMENTATION
#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"
#endif

#include <glm/gtx/transform.hpp>

// declaration of global variables
namespace
{
	const char* g_ModelName = "model";
	const char* g_ColorValueName = "objectColor";
	const char* g_TextureValueName = "objectTexture";
	const char* g_UseTextureName = "bUseTexture";
	const char* g_UseLightingName = "bUseLighting";
}

/***********************************************************
 *  SceneManager()
 *
 *  The constructor for the class
 ***********************************************************/
SceneManager::SceneManager(ShaderManager *pShaderManager)
{
	m_pShaderManager = pShaderManager;
	m_basicMeshes = new ShapeMeshes();
	
	// initialize the texture collection
	for (int i = 0; i < 16; i++)
	{
		m_textureIDs[i].tag = "/0";
		m_textureIDs[i].ID = -1;
	}
	m_loadedTextures = 0;

	// Initialize collision boxes vector
	m_collisionBoxes = std::vector<AABB>();
}

/***********************************************************
 *  ~SceneManager()
 *
 *  The destructor for the class
 ***********************************************************/
SceneManager::~SceneManager()
{
	m_pShaderManager = NULL;
	delete m_basicMeshes;
	m_basicMeshes = NULL;
	// destroy the created OpenGL textures
	DestroyGLTextures();
}

/***********************************************************
 *  CreateGLTexture()
 *
 *  This method is used for loading textures from image files,
 *  configuring the texture mapping parameters in OpenGL,
 *  generating the mipmaps, and loading the read texture into
 *  the next available texture slot in memory.
 ***********************************************************/
bool SceneManager::CreateGLTexture(const char* filename, std::string tag)
{
	int width = 0;
	int height = 0;
	int colorChannels = 0;
	GLuint textureID = 0;

	// indicate to always flip images vertically when loaded
	stbi_set_flip_vertically_on_load(true);

	// try to parse the image data from the specified image file
	unsigned char* image = stbi_load(
		filename,
		&width,
		&height,
		&colorChannels,
		0);

	// if the image was successfully read from the image file
	if (image)
	{
		std::cout << "Successfully loaded image:" << filename << ", width:" << width << ", height:" << height << ", channels:" << colorChannels << std::endl;

		glGenTextures(1, &textureID);
		glBindTexture(GL_TEXTURE_2D, textureID);

		// set the texture wrapping parameters
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		// set texture filtering parameters
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		// if the loaded image is in RGB format
		if (colorChannels == 3)
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, image);
		// if the loaded image is in RGBA format - it supports transparency
		else if (colorChannels == 4)
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
		else
		{
			std::cout << "Not implemented to handle image with " << colorChannels << " channels" << std::endl;
			return false;
		}

		// generate the texture mipmaps for mapping textures to lower resolutions
		glGenerateMipmap(GL_TEXTURE_2D);

		// free the image data from local memory
		stbi_image_free(image);
		glBindTexture(GL_TEXTURE_2D, 0); // Unbind the texture

		// register the loaded texture and associate it with the special tag string
		m_textureIDs[m_loadedTextures].ID = textureID;
		m_textureIDs[m_loadedTextures].tag = tag;
		m_loadedTextures++;

		return true;
	}

	std::cout << "Could not load image:" << filename << std::endl;

	// Error loading the image
	return false;
}

/***********************************************************
 *  BindGLTextures()
 *
 *  This method is used for binding the loaded textures to
 *  OpenGL texture memory slots.  There are up to 16 slots.
 ***********************************************************/
void SceneManager::BindGLTextures()
{
	for (int i = 0; i < m_loadedTextures; i++)
	{
		// bind textures on corresponding texture units
		glActiveTexture(GL_TEXTURE0 + i);
		glBindTexture(GL_TEXTURE_2D, m_textureIDs[i].ID);
	}
}

/***********************************************************
 *  DestroyGLTextures()
 *
 *  This method is used for freeing the memory in all the
 *  used texture memory slots.
 ***********************************************************/
void SceneManager::DestroyGLTextures()
{
	for (int i = 0; i < m_loadedTextures; i++)
	{
		glGenTextures(1, &m_textureIDs[i].ID);
	}
}

/***********************************************************
 *  FindTextureID()
 *
 *  This method is used for getting an ID for the previously
 *  loaded texture bitmap associated with the passed in tag.
 ***********************************************************/
int SceneManager::FindTextureID(std::string tag)
{
	int textureID = -1;
	int index = 0;
	bool bFound = false;

	while ((index < m_loadedTextures) && (bFound == false))
	{
		if (m_textureIDs[index].tag.compare(tag) == 0)
		{
			textureID = m_textureIDs[index].ID;
			bFound = true;
		}
		else
			index++;
	}

	return(textureID);
}

/***********************************************************
 *  FindTextureSlot()
 *
 *  This method is used for getting a slot index for the previously
 *  loaded texture bitmap associated with the passed in tag.
 ***********************************************************/
int SceneManager::FindTextureSlot(std::string tag)
{
	int textureSlot = -1;
	int index = 0;
	bool bFound = false;

	while ((index < m_loadedTextures) && (bFound == false))
	{
		if (m_textureIDs[index].tag.compare(tag) == 0)
		{
			textureSlot = index;
			bFound = true;
		}
		else
			index++;
	}

	return(textureSlot);
}

/***********************************************************
 *  FindMaterial()
 *
 *  This method is used for getting a material from the previously
 *  defined materials list that is associated with the passed in tag.
 ***********************************************************/
bool SceneManager::FindMaterial(std::string tag, OBJECT_MATERIAL& material)
{
	if (m_objectMaterials.size() == 0)
	{
		return(false);
	}

	int index = 0;
	bool bFound = false;
	while ((index < m_objectMaterials.size()) && (bFound == false))
	{
		if (m_objectMaterials[index].tag.compare(tag) == 0)
		{
			bFound = true;
			material.ambientColor = m_objectMaterials[index].ambientColor;
			material.ambientStrength = m_objectMaterials[index].ambientStrength;
			material.diffuseColor = m_objectMaterials[index].diffuseColor;
			material.specularColor = m_objectMaterials[index].specularColor;
			material.shininess = m_objectMaterials[index].shininess;
		}
		else
		{
			index++;
		}
	}

	return(true);
}

/***********************************************************
 *  SetTransformations()
 *
 *  This method is used for setting the transform buffer
 *  using the passed in transformation values.
 ***********************************************************/
void SceneManager::SetTransformations(
	glm::vec3 scaleXYZ,
	float XrotationDegrees,
	float YrotationDegrees,
	float ZrotationDegrees,
	glm::vec3 positionXYZ)
{
	// variables for this method
	glm::mat4 modelView;
	glm::mat4 scale;
	glm::mat4 rotationX;
	glm::mat4 rotationY;
	glm::mat4 rotationZ;
	glm::mat4 translation;

	// set the scale value in the transform buffer
	scale = glm::scale(scaleXYZ);
	// set the rotation values in the transform buffer
	rotationX = glm::rotate(glm::radians(XrotationDegrees), glm::vec3(1.0f, 0.0f, 0.0f));
	rotationY = glm::rotate(glm::radians(YrotationDegrees), glm::vec3(0.0f, 1.0f, 0.0f));
	rotationZ = glm::rotate(glm::radians(ZrotationDegrees), glm::vec3(0.0f, 0.0f, 1.0f));
	// set the translation value in the transform buffer
	translation = glm::translate(positionXYZ);

	modelView = translation * rotationX * rotationY * rotationZ * scale;

	if (NULL != m_pShaderManager)
	{
		m_pShaderManager->setMat4Value(g_ModelName, modelView);
	}
}

/***********************************************************
 *  SetShaderColor()
 *
 *  This method is used for setting the passed in color
 *  into the shader for the next draw command
 ***********************************************************/
void SceneManager::SetShaderColor(
	float redColorValue,
	float greenColorValue,
	float blueColorValue,
	float alphaValue)
{
	// variables for this method
	glm::vec4 currentColor;

	currentColor.r = redColorValue;
	currentColor.g = greenColorValue;
	currentColor.b = blueColorValue;
	currentColor.a = alphaValue;

	if (NULL != m_pShaderManager)
	{
		m_pShaderManager->setIntValue(g_UseTextureName, false);
		m_pShaderManager->setVec4Value(g_ColorValueName, currentColor);
	}
}

/***********************************************************
 *  SetShaderTexture()
 *
 *  This method is used for setting the texture data
 *  associated with the passed in ID into the shader.
 ***********************************************************/
void SceneManager::SetShaderTexture(
	std::string textureTag)
{
	if (NULL != m_pShaderManager)
	{
		m_pShaderManager->setIntValue(g_UseTextureName, true);

		int textureID = -1;
		textureID = FindTextureSlot(textureTag);
		m_pShaderManager->setSampler2DValue(g_TextureValueName, textureID);
	}
}

/***********************************************************
 *  SetTextureUVScale()
 *
 *  This method is used for setting the texture UV scale
 *  values into the shader.
 ***********************************************************/
void SceneManager::SetTextureUVScale(float u, float v)
{
	if (NULL != m_pShaderManager)
	{
		m_pShaderManager->setVec2Value("UVscale", glm::vec2(u, v));
	}
}

/***********************************************************
 *  SetShaderMaterial()
 *
 *  This method is used for passing the material values
 *  into the shader.
 ***********************************************************/
void SceneManager::SetShaderMaterial(
	std::string materialTag)
{
	if (m_objectMaterials.size() > 0)
	{
		OBJECT_MATERIAL material;
		bool bReturn = false;

		bReturn = FindMaterial(materialTag, material);
		if (bReturn == true)
		{
			m_pShaderManager->setVec3Value("material.ambientColor", material.ambientColor);
			m_pShaderManager->setFloatValue("material.ambientStrength", material.ambientStrength);
			m_pShaderManager->setVec3Value("material.diffuseColor", material.diffuseColor);
			m_pShaderManager->setVec3Value("material.specularColor", material.specularColor);
			m_pShaderManager->setFloatValue("material.shininess", material.shininess);
		}
	}
}
/***********************************************************
  *  DefineObjectMaterials()
  *
  *  This method is used for configuring the various material
  *  settings for all of the objects within the 3D scene.
  ***********************************************************/
void SceneManager::DefineObjectMaterials()
{
	OBJECT_MATERIAL glassMaterial;
	glassMaterial.ambientColor = glm::vec3(0.3f, 0.3f, 0.3f);
	glassMaterial.ambientStrength = 0.3f;
	glassMaterial.diffuseColor = glm::vec3(0.15f, 0.15f, 0.15f);
	glassMaterial.specularColor = glm::vec3(0.6f, 0.6f, 0.6f);
	glassMaterial.shininess = 85.0;
	glassMaterial.tag = "glass";
	m_objectMaterials.push_back(glassMaterial);

	OBJECT_MATERIAL metalMaterial;
	metalMaterial.ambientColor = glm::vec3(0.2f, 0.2f, 0.2f);
	metalMaterial.ambientStrength = 0.3f;
	metalMaterial.diffuseColor = glm::vec3(0.2f, 0.2f, 0.2f);
	metalMaterial.specularColor = glm::vec3(0.2f, 0.2f, 0.2f);
	metalMaterial.shininess = 10.0;
	metalMaterial.tag = "metal";
	m_objectMaterials.push_back(metalMaterial);

	OBJECT_MATERIAL matteGlassMaterial;
	matteGlassMaterial.ambientColor = glm::vec3(0.2f, 0.2f, 0.2f);
	matteGlassMaterial.ambientStrength = 0.3f;
	matteGlassMaterial.diffuseColor = glm::vec3(0.2f, 0.2f, 0.2f);
	matteGlassMaterial.specularColor = glm::vec3(0.2f, 0.2f, 0.2f);
	matteGlassMaterial.shininess = 5.0;
	matteGlassMaterial.tag = "matteGlass";
	m_objectMaterials.push_back(matteGlassMaterial);

	OBJECT_MATERIAL woodMaterial;
	woodMaterial.ambientColor = glm::vec3(0.4f, 0.4f, 0.38f);
	woodMaterial.ambientStrength = 0.2f;
	woodMaterial.diffuseColor = glm::vec3(0.2f, 0.17f, 0.14f);
	woodMaterial.specularColor = glm::vec3(0.05f, 0.05f, 0.05f);
	woodMaterial.shininess = 0.3;
	woodMaterial.tag = "wood";
	m_objectMaterials.push_back(woodMaterial);

	OBJECT_MATERIAL plasticMaterial;
	plasticMaterial.ambientColor = glm::vec3(0.2f, 0.2f, 0.2f);
	plasticMaterial.ambientStrength = 0.2f;
	plasticMaterial.diffuseColor = glm::vec3(0.5f, 0.5f, 0.5f);
	plasticMaterial.specularColor = glm::vec3(0.4f, 0.4f, 0.4f);
	plasticMaterial.shininess = 0.5;
	plasticMaterial.tag = "plastic";
	m_objectMaterials.push_back(plasticMaterial);


}

/***********************************************************
 *  SetupSceneLights()
 *
 *  This method is called to add and configure the light
 *  sources for the 3D scene.  There are up to 4 light sources.
 ***********************************************************/
void SceneManager::SetupSceneLights()
{
	m_pShaderManager->setBoolValue(g_UseLightingName, true);

	// Light 1 (Main Light)
	m_pShaderManager->setVec3Value("lightSources[0].position", -50.0f, 50.0f, -10.0f);
	m_pShaderManager->setVec3Value("lightSources[0].ambientColor", 0.20f, 0.25f, 0.3f);  // Significantly reduced ambient intensity
	m_pShaderManager->setVec3Value("lightSources[0].diffuseColor", 0.4f, 0.4f, 0.4f);  // Significantly reduced diffuse intensity
	m_pShaderManager->setVec3Value("lightSources[0].specularColor", 0.4f, 0.4f, 0.4f); // Slightly reduced specular
	m_pShaderManager->setFloatValue("lightSources[0].focalStrength", 32.0f);
	m_pShaderManager->setFloatValue("lightSources[0].specularIntensity", 0.3f);         // Further reduced specular intensity

	// Light 2 (Fill Light)
	m_pShaderManager->setVec3Value("lightSources[1].position", 20.0f, 50.0f, -10.0f);    // Complementary angle
	m_pShaderManager->setVec3Value("lightSources[1].ambientColor", 0.3f, 0.3f, 0.3f);  // Further reduced ambient intensity
	m_pShaderManager->setVec3Value("lightSources[1].diffuseColor", 0.2f, 0.2f, 0.2f);  // Further reduced diffuse intensity
	m_pShaderManager->setVec3Value("lightSources[1].specularColor", 0.4f, 0.4f, 0.4f); // Further reduced specular
	m_pShaderManager->setFloatValue("lightSources[1].focalStrength", 32.0f);
	m_pShaderManager->setFloatValue("lightSources[1].specularIntensity", 0.1f);

	// Enabling the use of lighting
	m_pShaderManager->setBoolValue("bUseLighting", true);

}
/***********************************************************
  *  LoadSceneTextures()
  *
  *  This method is used for preparing the 3D scene by loading
  *  the shapes, textures in memory to support the 3D scene
  *  rendering
  ***********************************************************/
void SceneManager::LoadSceneTextures()
{
	// Load the texture for the keyboard
	CreateGLTexture("../../Utilities/textures/keyboard.jpg", "keyboard");

	// Load the texture for the keyboard
	CreateGLTexture("../../Utilities/textures/top.jpg", "top");

	// Load the texture for the keyboard
	CreateGLTexture("../../Utilities/textures/base.jpg", "base");

	// Load the texture for the keyboard
	CreateGLTexture("../../Utilities/textures/trackpad.jpg", "trackpad");

	// Load the birch wood texture for the pen stand
	CreateGLTexture("../../Utilities/textures/yellowwood.jpg", "yellowwood");

	// Load the texture for the back of the phone
	CreateGLTexture("../../Utilities/textures/phoneback.jpg", "phoneback");

	// Load the texture for the side of the phone
	CreateGLTexture("../../Utilities/textures/phoneside.jpg", "phoneside");

	BindGLTextures();
}

/**************************************************************/
/*** STUDENTS CAN MODIFY the code in the methods BELOW for  ***/
/*** preparing and rendering their own 3D replicated scenes.***/
/*** Please refer to the code in the OpenGL sample project  ***/
/*** for assistance.                                        ***/
/**************************************************************/

/***********************************************************
 *  AddCollisionBox()
 *
 *  This method is used for adding a collision box to the scene
 *  by specifying the minimum and maximum corner coordinates
 ***********************************************************/
void SceneManager::AddCollisionBox(const glm::vec3& min, const glm::vec3& max, const std::string& name)
{
	AABB box;
	box.min = min;
	box.max = max;
	box.name = name;
	m_collisionBoxes.push_back(box);
}

/***********************************************************
 *  AddExactCollisionBox()
 *
 *  This method is used for adding a collision box that exactly
 *  matches the size, position, and rotation of a rendered object.
 *  It automatically handles dimension swapping for common rotations.
 ***********************************************************/
void SceneManager::AddExactCollisionBox(const glm::vec3& scale, float rotX, float rotY, float rotZ,
	const glm::vec3& position, const std::string& name)
{
	glm::vec3 halfSize = scale * 0.5f;

	// Adjust for rotations (simplified - for complex rotations you'd need full matrix math)
	if (rotX == 90.0f || rotX == 270.0f) {
		// X rotation swaps Y and Z
		std::swap(halfSize.y, halfSize.z);
	}
	if (rotY == 90.0f || rotY == 270.0f) {
		// Y rotation swaps X and Z
		std::swap(halfSize.x, halfSize.z);
	}
	if (rotZ == 90.0f || rotZ == 270.0f) {
		// Z rotation swaps X and Y
		std::swap(halfSize.x, halfSize.y);
	}

	AddCollisionBox(
		position - halfSize,
		position + halfSize,
		name
	);
}


/***********************************************************
 *  PrepareScene()
 *
 *  This method is used for preparing the 3D scene by loading
 *  the shapes, textures in memory to support the 3D scene 
 *  rendering
 ***********************************************************/
void SceneManager::PrepareScene()
{
	LoadSceneTextures();
	DefineObjectMaterials();
	SetupSceneLights();
	// only one instance of a particular mesh needs to be
	// loaded in memory no matter how many times it is drawn
	// in the rendered 3D scene

	m_basicMeshes->LoadPlaneMesh();
	m_basicMeshes->LoadBoxMesh();
	m_basicMeshes->LoadCylinderMesh();
}

/***********************************************************
 *  RenderScene()
 *
 *  This method is used for rendering the 3D scene by 
 *  transforming and drawing the basic 3D shapes
 ***********************************************************/
void SceneManager::RenderScene()
{
	// Clear previous collision boxes at start of render
	m_collisionBoxes.clear();

	// declare the variables for the transformations
	glm::vec3 scaleXYZ;
	float XrotationDegrees = 0.0f;
	float YrotationDegrees = 0.0f;
	float ZrotationDegrees = 0.0f;
	glm::vec3 positionXYZ;

	/*** Set needed transformations before drawing the basic mesh.  ***/
	/*** This same ordering of code should be used for transforming ***/
	/*** and drawing all the basic 3D shapes.						***/
	/******************************************************************/
	// set the XYZ scale for the mesh
	scaleXYZ = glm::vec3(50.0f, 1.0f, 20.0f);

	// set the XYZ rotation for the mesh
	XrotationDegrees = 0.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f;

	// set the XYZ position for the mesh
	positionXYZ = glm::vec3(-10.0f, -10.0f, -10.0f);

	// set the transformations into memory to be used on the drawn meshes
	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ);

	SetShaderColor(0.95f, 0.95f, 0.95f, 1.0f);
	SetShaderMaterial("wood");

	// draw the mesh with transformation values
	m_basicMeshes->DrawPlaneMesh();

	// Floor collision
	AddExactCollisionBox(
		glm::vec3(100.0f, 2.0f, 40.0f),
		0.0f, 0.0f, 0.0f,
		glm::vec3(-10.0f, -10.0f, -10.0f),
		"floor"
	);

	/****************************************************************/

	scaleXYZ = glm::vec3(50.0f, 1.0f, 30.0f);

	// set the XYZ rotation for the mesh
	XrotationDegrees = 90.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f;

	// set the XYZ position for the mesh
	positionXYZ = glm::vec3(-10.0f, 20.0f, -30.0f);

	// set the transformations into memory to be used on the drawn meshes
	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ);

	SetShaderColor(0.9f, 0.9f, 0.9f, 1.0f);
	SetShaderMaterial("wood");

	// draw the mesh with transformation values
	m_basicMeshes->DrawPlaneMesh();

	// Wall collision
	AddExactCollisionBox(
		glm::vec3(100.0f, 2.0f, 60.0f),
		90.0f, 0.0f, 0.0f,
		glm::vec3(-10.0f, 20.0f, -30.0f),
		"wall"
	);

	/****************************************************************/

	// Laptop Base
	scaleXYZ = glm::vec3(18.0f, 0.4f, 11.0f);
	positionXYZ = glm::vec3(-10.0f, -9.79f, -9.4f);
	SetTransformations(scaleXYZ, 0.0f, 0.0f, 0.0f, positionXYZ);
	SetShaderColor(1, 1, 1, 1);
	SetShaderTexture("base");
	SetShaderMaterial("metal");
	m_basicMeshes->DrawBoxMesh();
	AddExactCollisionBox(
		glm::vec3(18.0f, 0.4f, 11.0f),
		0.0f, 0.0f, 0.0f,
		glm::vec3(-10.0f, -9.79f, -9.4f),
		"laptop_base"
	);

	// Laptop Top
	scaleXYZ = glm::vec3(18.0f, 0.3f, 12.0f);
	positionXYZ = glm::vec3(-10.0f, -3.7f, -15.0f);
	SetTransformations(scaleXYZ, 90.0f, 0.0f, 0.0f, positionXYZ);
	SetShaderColor(1, 1, 1, 1);
	SetShaderTexture("top");
	SetShaderMaterial("metal");
	m_basicMeshes->DrawBoxMesh();
	AddExactCollisionBox(
		glm::vec3(18.0f, 0.3f, 12.0f),
		90.0f, 0.0f, 0.0f,
		glm::vec3(-10.0f, -3.7f, -15.0f),
		"laptop_screen"
	);

	scaleXYZ = glm::vec3(18.01f, 0.29f, 12.01f);
	positionXYZ = glm::vec3(-10.0f, -3.7f, -15.0f);
	SetTransformations(scaleXYZ, 90.0f, 0.0f, 0.0f, positionXYZ);
	SetShaderColor(1, 1, 1, 1);
	SetShaderTexture("base");
	SetShaderMaterial("metal");
	m_basicMeshes->DrawBoxMesh();

	// Laptop Screen Border
	scaleXYZ = glm::vec3(9.0f, 0.1f, 6.0f);
	positionXYZ = glm::vec3(-10.0f, -3.7f, -14.84f);
	SetTransformations(scaleXYZ, 90.0f, 0.0f, 00.0f, positionXYZ);
	SetShaderColor(0.0f, 0.0f, 0.0f, 1.0f);
	SetShaderMaterial("plastic");
	m_basicMeshes->DrawPlaneMesh();

	// Laptop Screen
	scaleXYZ = glm::vec3(8.6f, 0.1f, 5.4f);
	positionXYZ = glm::vec3(-10.0f, -3.7f, -14.82f);
	SetTransformations(scaleXYZ, 90.0f, 0.0f, 00.0f, positionXYZ);
	SetShaderColor(1, 1, 1, 1);
	SetShaderMaterial("glass");
	m_basicMeshes->DrawPlaneMesh();

	// Laptop Keyboard
	scaleXYZ = glm::vec3(8.0f, 0.1f, 2.7f);
	positionXYZ = glm::vec3(-10.0f, -9.58f, -11.0f);
	SetTransformations(scaleXYZ, 0.0f, 0.0f, 0.0f, positionXYZ);
	SetShaderColor(1, 1, 1, 1);
	SetShaderTexture("keyboard");
	SetShaderMaterial("plastic");
	m_basicMeshes->DrawPlaneMesh();

	// Laptop Trackpad
	scaleXYZ = glm::vec3(3.5f, 0.1f, 1.9f);
	positionXYZ = glm::vec3(-10.0f, -9.58f, -6.0f);
	SetTransformations(scaleXYZ, 0.0f, 0.0f, 0.0f, positionXYZ);
	SetShaderColor(1, 1, 1, 1);
	SetShaderTexture("trackpad");
	SetShaderMaterial("metal");
	m_basicMeshes->DrawPlaneMesh();


	// Pen stand Front
	scaleXYZ = glm::vec3(3.0f, 5.0f, 0.3f);
	positionXYZ = glm::vec3(-25.0f, -7.48f, -15.0f);
	SetTransformations(scaleXYZ, 0.0f, 0.0f, 0.0f, positionXYZ);
	SetShaderColor(1, 1, 1, 1);
	SetShaderTexture("yellowwood");
	SetShaderMaterial("wood");
	m_basicMeshes->DrawBoxMesh();
	AddExactCollisionBox(
		glm::vec3(6.0f, 10.0f, 0.6f),
		0.0f, 0.0f, 0.0f,
		glm::vec3(-25.0f, -7.48f, -15.0f),
		"pen_stand_front"
	);

	// Back
	scaleXYZ = glm::vec3(3.0f, 5.0f, 0.3f);
	positionXYZ = glm::vec3(-25.0f, -7.48f, -12.0f);
	SetTransformations(scaleXYZ, 0.0f, 0.0f, 0.0f, positionXYZ);
	SetShaderColor(1, 1, 1, 1);
	SetShaderTexture("yellowwood");
	SetShaderMaterial("wood");
	m_basicMeshes->DrawBoxMesh();
	AddExactCollisionBox(
		glm::vec3(6.0f, 10.0f, 0.6f),
		0.0f, 0.0f, 0.0f,
		glm::vec3(-25.0f, -7.48f, -12.0f),
		"pen_stand_back"
	);

	// Right side
	scaleXYZ = glm::vec3(3.0f, 5.0f, 0.3f);
	positionXYZ = glm::vec3(-23.5f, -7.48f, -13.5f);
	SetTransformations(scaleXYZ, 0.0f, 90.0f, 0.0f, positionXYZ);
	SetShaderColor(1, 1, 1, 1);
	SetShaderTexture("yellowwood");
	SetShaderMaterial("wood");
	m_basicMeshes->DrawBoxMesh();
	AddExactCollisionBox(
		glm::vec3(6.0f, 10.0f, 0.6f),
		0.0f, 90.0f, 0.0f,
		glm::vec3(-23.5f, -7.48f, -13.5f),
		"pen_stand_right"
	);

	// Left side 
	scaleXYZ = glm::vec3(3.0f, 5.0f, 0.3f);
	positionXYZ = glm::vec3(-26.5f, -7.48f, -13.5f);
	SetTransformations(scaleXYZ, 0.0f, 90.0f, 0.0f, positionXYZ);
	SetShaderColor(1, 1, 1, 1);
	SetShaderTexture("yellowwood");
	SetShaderMaterial("wood");
	m_basicMeshes->DrawBoxMesh();
	AddExactCollisionBox(
		glm::vec3(6.0f, 10.0f, 0.6f),
		0.0f, 90.0f, 0.0f,
		glm::vec3(-26.5f, -7.48f, -13.5f),
		"pen_stand_left"
	);

	// Bottom
	scaleXYZ = glm::vec3(3.0f, 3.0f, 0.3f);
	positionXYZ = glm::vec3(-25.0f, -9.8f, -13.5f);
	SetTransformations(scaleXYZ, 90.0f, 0.0f, 0.0f, positionXYZ);
	SetShaderColor(1, 1, 1, 1);
	SetShaderTexture("yellowwood");
	SetShaderMaterial("wood");
	m_basicMeshes->DrawBoxMesh();
	AddExactCollisionBox(
		glm::vec3(6.0f, 6.0f, 0.6f),
		90.0f, 0.0f, 0.0f,
		glm::vec3(-25.0f, -9.8f, -13.5f),
		"pen_stand_bottom"
	);
	

	// Pens
	scaleXYZ = glm::vec3(0.18f, 7.0f, 0.18f);
	positionXYZ = glm::vec3(-25.0f, -9.6f, -13.6f);
	SetTransformations(scaleXYZ, 15.0f, 0.0f, 5.0f, positionXYZ);
	SetShaderColor(0.1f, 0.1f, 0.1f, 1.0f);
	SetShaderMaterial("plastic");
	m_basicMeshes->DrawCylinderMesh();

	scaleXYZ = glm::vec3(0.18f, 7.2f, 0.18f);
	positionXYZ = glm::vec3(-25.0f, -9.6f, -13.8f);
	SetTransformations(scaleXYZ, -10.0f, 0.0f, -5.0f, positionXYZ);
	SetShaderColor(0.1f, 0.1f, 0.1f, 1.0f);
	SetShaderMaterial("plastic");
	m_basicMeshes->DrawCylinderMesh();

	scaleXYZ = glm::vec3(0.18f, 7.0f, 0.18f);
	positionXYZ = glm::vec3(-24.3f, -9.6f, -13.6f);
	SetTransformations(scaleXYZ, 15.0f, 5.0f, -5.0f, positionXYZ);
	SetShaderColor(0.1f, 0.1f, 0.1f, 1.0f);
	SetShaderMaterial("plastic");
	m_basicMeshes->DrawCylinderMesh();

	scaleXYZ = glm::vec3(0.18f, 6.8f, 0.18f);
	positionXYZ = glm::vec3(-25.3f, -9.6f, -13.5f);
	SetTransformations(scaleXYZ, -5.0f, -5.0f, 10.0f, positionXYZ);
	SetShaderColor(0.1f, 0.1f, 0.1f, 1.0f);
	SetShaderMaterial("plastic");
	m_basicMeshes->DrawCylinderMesh();


	// Phone
	scaleXYZ = glm::vec3(3.0f, 0.2f, 6.0f);
	positionXYZ = glm::vec3(4.0f, -9.88f, -8.0f);
	SetTransformations(scaleXYZ, 0.0f, 0.0f, 0.0f, positionXYZ);
	SetShaderColor(1, 1, 1, 1);
	SetShaderTexture("phoneback");
	SetShaderMaterial("matteGlass");
	m_basicMeshes->DrawBoxMesh();

	scaleXYZ = glm::vec3(3.01f, 0.19f, 6.01f);
	positionXYZ = glm::vec3(4.0f, -9.88f, -8.0f);
	SetTransformations(scaleXYZ, 0.0f, 0.0f, 0.0f, positionXYZ);
	SetShaderColor(1, 1, 1, 1);
	SetShaderTexture("phoneside");
	SetShaderMaterial("matteGlass");
	m_basicMeshes->DrawBoxMesh();
}