/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sysml.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sysml.api.DMLScript;

/**
 * Automatically generate script documentation as markdown.
 * 
 */
public class GenerateScriptDocumentation {

	public static final String SOURCE = "scripts";
	// public static final String DESTINATION = "target";
	// public static final String DESTINATION = "docs" + File.separator +
	// "scripts";
	public static final String DESTINATION = "docs";
	// public static final String BASE_DEST_PACKAGE = "org.apache.sysml";

	public static String source = SOURCE;
	public static String destination = DESTINATION;
	public static boolean skipStaging = true;
	public static boolean skipPerfTest = true;
	public static boolean skipObsolete = true;

	// @formatter:off
	public    static    boolean    skipObsolete2 = true;
	// @formatter:on

	public static void main(String[] args) throws Throwable {
		if (args.length == 2) {
			source = args[0];
			destination = args[1];
		} else if (args.length == 1) {
			source = args[0];
		}
		try {
			// DMLScript.VALIDATOR_IGNORE_ISSUES = true;
			System.out.println("*****************************************");
			System.out.println("**** Script Documentation Generation ****");
			System.out.println("******************************************");
			System.out.println("Source: " + source);
			System.out.println("Destination: " + destination);
			recurseDirectoriesForDocumentationGeneration(source, false, null);
		} finally {
			// DMLScript.VALIDATOR_IGNORE_ISSUES = false;
		}
	}

	/**
	 * Whether or not the directory (and subdirectories of the directory) should
	 * be skipped.
	 * 
	 * @param dir
	 *            path to directory to check
	 * @param displayMessage
	 *            if {@code true}, display skip information to standard output
	 * @return {@code true} if the directory should be skipped, {@code false}
	 *         otherwise
	 */
	public static boolean skipDir(File dir, boolean displayMessage) {
		if ("staging".equalsIgnoreCase(dir.getName()) && skipStaging) {
			if (displayMessage) {
				System.out.println("Skipping staging directory: " + dir.getPath());
			}
			return true;
		}
		if ("perftest".equalsIgnoreCase(dir.getName()) && skipPerfTest) {
			if (displayMessage) {
				System.out.println("Skipping perftest directory: " + dir.getPath());
			}
			return true;
		}
		if ("obsolete".equalsIgnoreCase(dir.getName()) && skipObsolete) {
			if (displayMessage) {
				System.out.println("Skipping obsolete directory: " + dir.getPath());
			}
			return true;
		}
		return false;
	}

	public static void generatePackageHeader(StringBuilder sb, String dirPath) {

		String readmeString = obtainReadmeAsStringIfExists(dirPath);

		String title = null;
		String description = null;
		if ((readmeString == null) || (!readmeString.contains("# "))) {
			title = dirPath.substring(dirPath.indexOf(File.separator) + 1);
			description = title;
		} else if (readmeString.contains("# ")) {
			int titleStart = readmeString.indexOf("# ") + 2;
			int titleEnd = readmeString.indexOf("\n", titleStart);
			title = readmeString.substring(titleStart, titleEnd);
			readmeString = readmeString.replaceFirst("# " + title, "");

			int descriptionStart = titleEnd + 1 - ("# " + title).length();
			int descriptionEnd = readmeString.indexOf("#", descriptionStart);

			description = readmeString.substring(descriptionStart, descriptionEnd);
			readmeString = StringUtils.replace(readmeString, description, "", 1);
			description = description.trim();

			readmeString = StringUtils.removeFirst(readmeString, "(?s)<!--.*?-->");
		}

		sb.append("---\n");
		sb.append("layout: global\n");
		sb.append("title: " + title + "\n");
		sb.append("description: " + description + "\n");
		sb.append("---\n\n");

		sb.append("<!--\n");
		sb.append("{% comment %}\n");
		sb.append("Licensed to the Apache Software Foundation (ASF) under one or more\n");
		sb.append("contributor license agreements.  See the NOTICE file distributed with\n");
		sb.append("this work for additional information regarding copyright ownership.\n");
		sb.append("The ASF licenses this file to you under the Apache License, Version 2.0\n");
		sb.append("(the \"License\"); you may not use this file except in compliance with\n");
		sb.append("the License.  You may obtain a copy of the License at\n");
		sb.append("\n");
		sb.append("http://www.apache.org/licenses/LICENSE-2.0\n");
		sb.append("\n");
		sb.append("Unless required by applicable law or agreed to in writing, software\n");
		sb.append("distributed under the License is distributed on an \"AS IS\" BASIS,\n");
		sb.append("WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n");
		sb.append("See the License for the specific language governing permissions and\n");
		sb.append("limitations under the License.\n");
		sb.append("{% endcomment %}\n");
		sb.append("-->\n\n");

		sb.append(description);
		sb.append("\n\n");

		sb.append("* This will become a table of contents (this text will be scraped).\n");
		sb.append("{:toc}\n\n");
		sb.append("<br/>\n\n");

		if (readmeString != null) {
			sb.append(readmeString);
		}
		sb.append("\n");
	}

	public static String obtainReadmeAsStringIfExists(String dirPath) {
		File readmeFile = new File(dirPath + File.separator + "README.md");
		String readmeString = null;
		if (readmeFile.exists()) {
			try {
				readmeString = FileUtils.readFileToString(readmeFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return readmeString;
	}

	public static void generateSubPackageHeader(StringBuilder sb, String dirPath) {

		sb.append("\n---\n\n");
		sb.append("# " + dirPath.substring(dirPath.indexOf("/") + 1));
		sb.append("\n\n");

		String readmeString = obtainReadmeAsStringIfExists(dirPath);
		if (readmeString != null) {

			readmeString = StringUtils.removeFirst(readmeString, "(?s)<!--.*?-->");
			readmeString = readmeString.replace("\n#", "\n##");

			readmeString = "<div style=\"background-color: #f5f5f5; border: 1px solid #ccc; border-radius: 4px; color: #333; padding: 10px;\" markdown=\"1\">"
					+ readmeString + "\n\n</div>\n\n";

			sb.append(readmeString);
		}
		sb.append("\n");
	}

	/**
	 * Recursively traverse the directories to create documentation for the
	 * script files.
	 * 
	 * @param dirPath
	 *            path to directory
	 * @param isTopPackage
	 *            true if this directory is a top-level package (like nn)
	 * @param sb
	 *            StringBuilder with which to output the documentation page
	 */
	public static void recurseDirectoriesForDocumentationGeneration(String dirPath, boolean isTopPackage,
			StringBuilder sb) {
		StringBuilder packageSb = null;
		if (isTopPackage) {
			packageSb = new StringBuilder();
			generatePackageHeader(packageSb, dirPath);
		} else if (sb != null) {
			generateSubPackageHeader(sb, dirPath);
		}

		File dir = new File(dirPath);

		iterateScriptFilesInDirectory(dir, sb);

		File[] subdirs = dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory();
			}
		});
		for (File subdir : subdirs) {
			String subdirpath = dirPath + File.separator + subdir.getName();
			if (skipDir(subdir, true)) {
				continue;
			}

			if (source.equals(dirPath)) {
				recurseDirectoriesForDocumentationGeneration(subdirpath, true, packageSb);
			} else {
				recurseDirectoriesForDocumentationGeneration(subdirpath, false, packageSb);
			}
		}

		if (isTopPackage) {
			String packageDocs = packageSb.toString();
			System.out.println("Package Docs: " + packageDocs);
			String filename = DESTINATION + File.separator + dirPath.substring(dirPath.indexOf(File.separator) + 1)
					+ ".md";
			File f = new File(filename);
			try {
				FileUtils.writeStringToFile(f, packageDocs);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Iterate through the script files in a directory and create documentation
	 * for each script file.
	 * 
	 * @param dir
	 *            the directory to iterate through
	 */
	public static void iterateScriptFilesInDirectory(File dir, StringBuilder sb) {
		if (sb == null) {
			return;
		}
		File[] scriptFiles = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return (name.toLowerCase().endsWith(".dml") || name.toLowerCase().endsWith(".pydml"));
			}
		});
		for (File scriptFile : scriptFiles) {
			String scriptFilePath = scriptFile.getPath();
			System.out.println("SCRIPT FILE PATH:" + scriptFilePath);
			sb.append("---\n\n");
			String scriptFilePathRemoveScripts = scriptFilePath.substring(scriptFilePath.indexOf("/") + 1);
			sb.append("# " + scriptFilePathRemoveScripts);
			sb.append("\n\n");
			// createScriptClass(scriptFilePath);
		}
	}

	// /**
	// * Obtain the relative package for a script file. For example,
	// * {@code scripts/algorithms/LinearRegCG.dml} resolves to
	// * {@code scripts.algorithms}.
	// *
	// * @param scriptFilePath
	// * the path to a script file
	// * @return the relative package for a script file
	// */
	// public static String scriptFilePathToPackageNoBase(String scriptFilePath)
	// {
	// String p = scriptFilePath;
	// p = p.substring(0, p.lastIndexOf(File.separator));
	// p = p.replace("-", "_");
	// p = p.replace(File.separator, ".");
	// p = p.toLowerCase();
	// return p;
	// }
	//
	// /**
	// * Obtain the simple class name for a script file. For example,
	// * {@code scripts/algorithms/LinearRegCG} resolves to {@code LinearRegCG}.
	// *
	// * @param scriptFilePath
	// * the path to a script file
	// * @return the simple class name for a script file
	// */
	// public static String scriptFilePathToSimpleClassName(String
	// scriptFilePath) {
	// String c = scriptFilePath;
	// c = c.substring(c.lastIndexOf(File.separator) + 1);
	// c = c.replace("-", "_");
	// c = c.substring(0, 1).toUpperCase() + c.substring(1);
	// c = c.substring(0, c.indexOf("."));
	// return c;
	// }
	//
	// /**
	// * Obtain the relative full class name for a script file. For example,
	// * {@code scripts/algorithms/LinearRegCG.dml} resolves to
	// * {@code scripts.algorithms.LinearRegCG}.
	// *
	// * @param scriptFilePath
	// * the path to a script file
	// * @return the relative full class name for a script file
	// */
	// public static String scriptFilePathToFullClassNameNoBase(String
	// scriptFilePath) {
	// String p = scriptFilePathToPackageNoBase(scriptFilePath);
	// String c = scriptFilePathToSimpleClassName(scriptFilePath);
	// return p + "." + c;
	// }
	//
	// /**
	// * Convert a script file to a Java class that extends the MLContext API's
	// * Script class.
	// *
	// * @param scriptFilePath
	// * the path to a script file
	// */
	// public static void createScriptClass(String scriptFilePath) {
	// try {
	// String fullScriptClassName = BASE_DEST_PACKAGE + "." +
	// scriptFilePathToFullClassNameNoBase(scriptFilePath);
	// System.out.println("Generating Class: " + fullScriptClassName);
	// ClassPool pool = ClassPool.getDefault();
	// CtClass ctNewScript = pool.makeClass(fullScriptClassName);
	//
	// CtClass ctScript = pool.get(Script.class.getName());
	// ctNewScript.setSuperclass(ctScript);
	//
	// CtConstructor ctCon = new CtConstructor(null, ctNewScript);
	// ctCon.setBody(scriptConstructorBody(scriptFilePath));
	// ctNewScript.addConstructor(ctCon);
	//
	// addFunctionMethods(scriptFilePath, ctNewScript);
	//
	// ctNewScript.writeFile(destination);
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// } catch (RuntimeException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// } catch (CannotCompileException e) {
	// e.printStackTrace();
	// } catch (NotFoundException e) {
	// e.printStackTrace();
	// }
	//
	// }
	//
	// /**
	// * Create a DMLProgram from a script file.
	// *
	// * @param scriptFilePath
	// * the path to a script file
	// * @return the DMLProgram generated by the script file
	// */
	// public static DMLProgram dmlProgramFromScriptFilePath(String
	// scriptFilePath) {
	// String scriptString = fileToString(scriptFilePath);
	// Script script = new Script(scriptString);
	// ScriptExecutor se = new ScriptExecutor() {
	// @Override
	// public MLResults execute(Script script) {
	// setup(script);
	// parseScript();
	// return null;
	// }
	// };
	// se.execute(script);
	// DMLProgram dmlProgram = se.getDmlProgram();
	// return dmlProgram;
	// }
	//
	// /**
	// * Add methods to a derived script class to allow invocation of script
	// * functions.
	// *
	// * @param scriptFilePath
	// * the path to a script file
	// * @param ctNewScript
	// * the javassist compile-time class representation of a script
	// */
	// public static void addFunctionMethods(String scriptFilePath, CtClass
	// ctNewScript) {
	// try {
	// DMLProgram dmlProgram = dmlProgramFromScriptFilePath(scriptFilePath);
	// if (dmlProgram == null) {
	// System.out.println("Could not generate DML Program for: " +
	// scriptFilePath);
	// return;
	// }
	// Map<String, FunctionStatementBlock> defaultNsFsbsMap = dmlProgram
	// .getFunctionStatementBlocks(DMLProgram.DEFAULT_NAMESPACE);
	// List<FunctionStatementBlock> fsbs = new
	// ArrayList<FunctionStatementBlock>();
	// fsbs.addAll(defaultNsFsbsMap.values());
	// for (FunctionStatementBlock fsb : fsbs) {
	// ArrayList<Statement> sts = fsb.getStatements();
	// for (Statement st : sts) {
	// if (!(st instanceof FunctionStatement)) {
	// continue;
	// }
	// FunctionStatement fs = (FunctionStatement) st;
	//
	// String dmlFunctionCall = generateDmlFunctionCall(scriptFilePath, fs);
	// String functionCallMethod = generateFunctionCallMethod(scriptFilePath,
	// fs, dmlFunctionCall);
	//
	// CtMethod m = CtNewMethod.make(functionCallMethod, ctNewScript);
	// ctNewScript.addMethod(m);
	//
	// addDescriptionFunctionCallMethod(fs, scriptFilePath, ctNewScript, false);
	// addDescriptionFunctionCallMethod(fs, scriptFilePath, ctNewScript, true);
	// }
	// }
	// } catch (LanguageException e) {
	// System.out.println("Could not add function methods for " +
	// ctNewScript.getName());
	// } catch (CannotCompileException e) {
	// System.out.println("Could not add function methods for " +
	// ctNewScript.getName());
	// } catch (RuntimeException e) {
	// System.out.println("Could not add function methods for " +
	// ctNewScript.getName());
	// }
	// }
	//
	// /**
	// * Create a method that returns either: (1) the full function body, or (2)
	// * the function body up to the end of the documentation comment for the
	// * function. If (1) is generated, the method name will be followed by two
	// * underscores. If (2) is generated, the method name will be followed by
	// one
	// * underscore. If (2) is generated but no end of documentation comment is
	// * detected, the full function body will be displayed.
	// *
	// * @param fs
	// * a SystemML function statement
	// * @param scriptFilePath
	// * the path to a script file
	// * @param ctNewScript
	// * the javassist compile-time class representation of a script
	// * @param full
	// * if {@code true}, create method to return full function body;
	// * if {@code false}, create method to return the function body up
	// * to the end of the documentation comment
	// */
	// public static void addDescriptionFunctionCallMethod(FunctionStatement fs,
	// String scriptFilePath,
	// CtClass ctNewScript, boolean full) {
	//
	// try {
	// int bl = fs.getBeginLine();
	// int el = fs.getEndLine();
	// File f = new File(scriptFilePath);
	// List<String> lines = FileUtils.readLines(f);
	//
	// int end = el;
	// if (!full) {
	// for (int i = bl - 1; i < el; i++) {
	// String line = lines.get(i);
	// if (line.contains("*/")) {
	// end = i + 1;
	// break;
	// }
	// }
	// }
	// List<String> sub = lines.subList(bl - 1, end);
	// StringBuilder sb = new StringBuilder();
	// for (int i = 0; i < sub.size(); i++) {
	// String line = sub.get(i);
	// String escapeLine = StringEscapeUtils.escapeJava(line);
	// sb.append(escapeLine);
	// sb.append("\\n");
	// }
	//
	// String functionString = sb.toString();
	// String docFunctionCallMethod = generateDescriptionFunctionCallMethod(fs,
	// functionString, full);
	// CtMethod m = CtNewMethod.make(docFunctionCallMethod, ctNewScript);
	// ctNewScript.addMethod(m);
	//
	// } catch (IOException e) {
	// e.printStackTrace();
	// } catch (CannotCompileException e) {
	// e.printStackTrace();
	// }
	//
	// }
	//
	// /**
	// * Generate method for returning (1) the full function body, or (2) the
	// * function body up to the end of the documentation comment. (1) will have
	// * "__" appended to the end of the function name. (2) will have "_"
	// appended
	// * to the end of the function name.
	// *
	// * @param fs
	// * a SystemML function statement
	// * @param functionString
	// * either the full function body or the function body up to the
	// * end of the documentation comment
	// * @param full
	// * if {@code true}, append "__" to the end of the function name;
	// * if {@code false}, append "_" to the end of the function name
	// * @return string representation of the function description method
	// */
	// public static String
	// generateDescriptionFunctionCallMethod(FunctionStatement fs, String
	// functionString,
	// boolean full) {
	// StringBuilder sb = new StringBuilder();
	// sb.append("public String ");
	// sb.append(fs.getName());
	// if (full) {
	// sb.append("__");
	// } else {
	// sb.append("_");
	// }
	// sb.append("() {\n");
	// sb.append("String docString = \"" + functionString + "\";\n");
	// sb.append("return docString;\n");
	// sb.append("}\n");
	// return sb.toString();
	// }
	//
	// /**
	// * Obtain a string representation of a parameter type, where a Matrix or
	// * Frame is represented by its full class name.
	// *
	// * @param param
	// * the function parameter
	// * @return string representation of a parameter type
	// */
	// public static String getParamTypeAsString(DataIdentifier param) {
	// DataType dt = param.getDataType();
	// ValueType vt = param.getValueType();
	// if ((dt == DataType.SCALAR) && (vt == ValueType.INT)) {
	// return "long";
	// } else if ((dt == DataType.SCALAR) && (vt == ValueType.DOUBLE)) {
	// return "double";
	// } else if ((dt == DataType.SCALAR) && (vt == ValueType.BOOLEAN)) {
	// return "boolean";
	// } else if ((dt == DataType.SCALAR) && (vt == ValueType.STRING)) {
	// return "String";
	// } else if (dt == DataType.MATRIX) {
	// return "org.apache.sysml.api.mlcontext.Matrix";
	// } else if (dt == DataType.FRAME) {
	// return "org.apache.sysml.api.mlcontext.Frame";
	// }
	// return null;
	// }
	//
	// /**
	// * Obtain a string representation of a parameter type, where a Matrix or
	// * Frame is represented by its simple class name.
	// *
	// * @param param
	// * the function parameter
	// * @return string representation of a parameter type
	// */
	// public static String getSimpleParamTypeAsString(DataIdentifier param) {
	// DataType dt = param.getDataType();
	// ValueType vt = param.getValueType();
	// if ((dt == DataType.SCALAR) && (vt == ValueType.INT)) {
	// return "long";
	// } else if ((dt == DataType.SCALAR) && (vt == ValueType.DOUBLE)) {
	// return "double";
	// } else if ((dt == DataType.SCALAR) && (vt == ValueType.BOOLEAN)) {
	// return "boolean";
	// } else if ((dt == DataType.SCALAR) && (vt == ValueType.STRING)) {
	// return "String";
	// } else if (dt == DataType.MATRIX) {
	// return "Matrix";
	// } else if (dt == DataType.FRAME) {
	// return "Frame";
	// }
	// return null;
	// }
	//
	// /**
	// * Obtain the full class name for a class that encapsulates the outputs of
	// a
	// * function
	// *
	// * @param scriptFilePath
	// * the path to a script file
	// * @param fs
	// * a SystemML function statement
	// * @return the full class name for a class that encapsulates the outputs
	// of
	// * a function
	// */
	// public static String getFullFunctionOutputClassName(String
	// scriptFilePath, FunctionStatement fs) {
	// String p = scriptFilePath;
	// p = p.replace("-", "_");
	// p = p.replace(File.separator, ".");
	// p = p.toLowerCase();
	// p = p.substring(0, p.lastIndexOf("."));
	//
	// String c = fs.getName();
	// c = c.substring(0, 1).toUpperCase() + c.substring(1);
	// c = c + "_output";
	//
	// String functionOutputClassName = BASE_DEST_PACKAGE + "." + p + "." + c;
	// return functionOutputClassName;
	// }
	//
	// /**
	// * Create a class that encapsulates the outputs of a function.
	// *
	// * @param scriptFilePath
	// * the path to a script file
	// * @param fs
	// * a SystemML function statement
	// */
	// public static void createFunctionOutputClass(String scriptFilePath,
	// FunctionStatement fs) {
	//
	// try {
	// ArrayList<DataIdentifier> oparams = fs.getOutputParams();
	// // Note: if a function returns 1 output, simply output it rather
	// // than encapsulating it in a function output class
	// if ((oparams.size() == 0) || (oparams.size() == 1)) {
	// return;
	// }
	//
	// String fullFunctionOutputClassName =
	// getFullFunctionOutputClassName(scriptFilePath, fs);
	// System.out.println("Generating Class: " + fullFunctionOutputClassName);
	// ClassPool pool = ClassPool.getDefault();
	// CtClass ctFuncOut = pool.makeClass(fullFunctionOutputClassName);
	//
	// // add fields
	// for (int i = 0; i < oparams.size(); i++) {
	// DataIdentifier oparam = oparams.get(i);
	// String type = getParamTypeAsString(oparam);
	// String name = oparam.getName().toLowerCase();
	// String fstring = "public " + type + " " + name + ";";
	// CtField field = CtField.make(fstring, ctFuncOut);
	// ctFuncOut.addField(field);
	// }
	//
	// // add constructor
	// String simpleFuncOutClassName = fullFunctionOutputClassName
	// .substring(fullFunctionOutputClassName.lastIndexOf(".") + 1);
	// StringBuilder con = new StringBuilder();
	// con.append("public " + simpleFuncOutClassName + "(");
	// for (int i = 0; i < oparams.size(); i++) {
	// if (i > 0) {
	// con.append(", ");
	// }
	// DataIdentifier oparam = oparams.get(i);
	// String type = getParamTypeAsString(oparam);
	// String name = oparam.getName().toLowerCase();
	// con.append(type + " " + name);
	// }
	// con.append(") {\n");
	// for (int i = 0; i < oparams.size(); i++) {
	// DataIdentifier oparam = oparams.get(i);
	// String name = oparam.getName().toLowerCase();
	// con.append("this." + name + "=" + name + ";\n");
	// }
	// con.append("}\n");
	// String cstring = con.toString();
	// CtConstructor ctCon = CtNewConstructor.make(cstring, ctFuncOut);
	// ctFuncOut.addConstructor(ctCon);
	//
	// // add toString
	// StringBuilder s = new StringBuilder();
	// s.append("public String toString(){\n");
	// s.append("StringBuilder sb = new StringBuilder();\n");
	// for (int i = 0; i < oparams.size(); i++) {
	// DataIdentifier oparam = oparams.get(i);
	// String name = oparam.getName().toLowerCase();
	// s.append("sb.append(\"" + name + " (" +
	// getSimpleParamTypeAsString(oparam) + "): \" + " + name
	// + " + \"\\n\");\n");
	// }
	// s.append("String str = sb.toString();\nreturn str;\n");
	// s.append("}\n");
	// String toStr = s.toString();
	// CtMethod toStrMethod = CtNewMethod.make(toStr, ctFuncOut);
	// ctFuncOut.addMethod(toStrMethod);
	//
	// ctFuncOut.writeFile(destination);
	//
	// } catch (RuntimeException e) {
	// e.printStackTrace();
	// } catch (CannotCompileException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	//
	// /**
	// * Obtain method for invoking a script function.
	// *
	// * @param scriptFilePath
	// * the path to a script file
	// * @param fs
	// * a SystemML function statement
	// * @param dmlFunctionCall
	// * a string representing the invocation of a script function
	// * @return string representation of a method that performs a function call
	// */
	// public static String generateFunctionCallMethod(String scriptFilePath,
	// FunctionStatement fs,
	// String dmlFunctionCall) {
	//
	// createFunctionOutputClass(scriptFilePath, fs);
	//
	// StringBuilder sb = new StringBuilder();
	// sb.append("public ");
	//
	// // begin return type
	// ArrayList<DataIdentifier> oparams = fs.getOutputParams();
	// if (oparams.size() == 0) {
	// sb.append("void");
	// } else if (oparams.size() == 1) {
	// // if 1 output, no need to encapsulate it, so return the output
	// // directly
	// DataIdentifier oparam = oparams.get(0);
	// String type = getParamTypeAsString(oparam);
	// sb.append(type);
	// } else {
	// String fullFunctionOutputClassName =
	// getFullFunctionOutputClassName(scriptFilePath, fs);
	// sb.append(fullFunctionOutputClassName);
	// }
	// sb.append(" ");
	// // end return type
	//
	// sb.append(fs.getName());
	// sb.append("(");
	//
	// ArrayList<DataIdentifier> inputParams = fs.getInputParams();
	// for (int i = 0; i < inputParams.size(); i++) {
	// if (i > 0) {
	// sb.append(", ");
	// }
	// DataIdentifier inputParam = inputParams.get(i);
	// /*
	// * Note: Using Object is currently preferrable to using
	// * datatype/valuetype to explicitly set the input type to
	// * Integer/Double/Boolean/String since Object allows the automatic
	// * handling of things such as automatic conversions from longs to
	// * ints.
	// */
	// sb.append("Object ");
	// sb.append(inputParam.getName());
	// }
	//
	// sb.append(") {\n");
	// sb.append("String scriptString = \"" + dmlFunctionCall + "\";\n");
	// sb.append(
	// "org.apache.sysml.api.mlcontext.Script script = new
	// org.apache.sysml.api.mlcontext.Script(scriptString);\n");
	//
	// if ((inputParams.size() > 0) || (oparams.size() > 0)) {
	// sb.append("script");
	// }
	// for (int i = 0; i < inputParams.size(); i++) {
	// DataIdentifier inputParam = inputParams.get(i);
	// String name = inputParam.getName();
	// sb.append(".in(\"" + name + "\", " + name + ")");
	// }
	// for (int i = 0; i < oparams.size(); i++) {
	// DataIdentifier outputParam = oparams.get(i);
	// String name = outputParam.getName();
	// sb.append(".out(\"" + name + "\")");
	// }
	// if ((inputParams.size() > 0) || (oparams.size() > 0)) {
	// sb.append(";\n");
	// }
	//
	// sb.append("org.apache.sysml.api.mlcontext.MLResults results =
	// script.execute();\n");
	//
	// if (oparams.size() == 0) {
	// sb.append("return;\n");
	// } else if (oparams.size() == 1) {
	// DataIdentifier o = oparams.get(0);
	// DataType dt = o.getDataType();
	// ValueType vt = o.getValueType();
	// if ((dt == DataType.SCALAR) && (vt == ValueType.INT)) {
	// sb.append("long res = results.getLong(\"" + o.getName() + "\");\nreturn
	// res;\n");
	// } else if ((dt == DataType.SCALAR) && (vt == ValueType.DOUBLE)) {
	// sb.append("double res = results.getDouble(\"" + o.getName() +
	// "\");\nreturn res;\n");
	// } else if ((dt == DataType.SCALAR) && (vt == ValueType.BOOLEAN)) {
	// sb.append("boolean res = results.getBoolean(\"" + o.getName() +
	// "\");\nreturn res;\n");
	// } else if ((dt == DataType.SCALAR) && (vt == ValueType.STRING)) {
	// sb.append("String res = results.getString(\"" + o.getName() +
	// "\");\nreturn res;\n");
	// } else if (dt == DataType.MATRIX) {
	// sb.append("org.apache.sysml.api.mlcontext.Matrix res =
	// results.getMatrix(\"" + o.getName()
	// + "\");\nreturn res;\n");
	// } else if (dt == DataType.FRAME) {
	// sb.append("org.apache.sysml.api.mlcontext.Frame res =
	// results.getFrame(\"" + o.getName()
	// + "\");\nreturn res;\n");
	// }
	// } else {
	//
	// for (int i = 0; i < oparams.size(); i++) {
	// DataIdentifier outputParam = oparams.get(i);
	// String name = outputParam.getName().toLowerCase();
	// String type = getParamTypeAsString(outputParam);
	// DataType dt = outputParam.getDataType();
	// ValueType vt = outputParam.getValueType();
	// if ((dt == DataType.SCALAR) && (vt == ValueType.INT)) {
	// sb.append(type + " " + name + " = results.getLong(\"" +
	// outputParam.getName() + "\");\n");
	// } else if ((dt == DataType.SCALAR) && (vt == ValueType.DOUBLE)) {
	// sb.append(type + " " + name + " = results.getDouble(\"" +
	// outputParam.getName() + "\");\n");
	// } else if ((dt == DataType.SCALAR) && (vt == ValueType.BOOLEAN)) {
	// sb.append(type + " " + name + " = results.getBoolean(\"" +
	// outputParam.getName() + "\");\n");
	// } else if ((dt == DataType.SCALAR) && (vt == ValueType.STRING)) {
	// sb.append(type + " " + name + " = results.getString(\"" +
	// outputParam.getName() + "\");\n");
	// } else if (dt == DataType.MATRIX) {
	// sb.append(type + " " + name + " = results.getMatrix(\"" +
	// outputParam.getName() + "\");\n");
	// } else if (dt == DataType.FRAME) {
	// sb.append(type + " " + name + " = results.getFrame(\"" +
	// outputParam.getName() + "\");\n");
	// }
	// }
	// String ffocn = getFullFunctionOutputClassName(scriptFilePath, fs);
	// sb.append(ffocn + " res = new " + ffocn + "(");
	// for (int i = 0; i < oparams.size(); i++) {
	// if (i > 0) {
	// sb.append(", ");
	// }
	// DataIdentifier outputParam = oparams.get(i);
	// String name = outputParam.getName().toLowerCase();
	// sb.append(name);
	// }
	// sb.append(");\nreturn res;\n");
	// }
	//
	// sb.append("}\n");
	// return sb.toString();
	// }
	//
	// /**
	// * Obtain method for invoking a script function and returning the results
	// as
	// * an MLResults object. Currently this method is not used.
	// *
	// * @param scriptFilePath
	// * the path to a script file
	// * @param fs
	// * a SystemML function statement
	// * @param dmlFunctionCall
	// * a string representing the invocation of a script function
	// * @return string representation of a method that performs a function call
	// */
	// public static String generateFunctionCallMethodMLResults(String
	// scriptFilePath, FunctionStatement fs,
	// String dmlFunctionCall) {
	// StringBuilder sb = new StringBuilder();
	//
	// sb.append("public org.apache.sysml.api.mlcontext.MLResults ");
	// sb.append(fs.getName());
	// sb.append("(");
	//
	// ArrayList<DataIdentifier> inputParams = fs.getInputParams();
	// for (int i = 0; i < inputParams.size(); i++) {
	// if (i > 0) {
	// sb.append(", ");
	// }
	// DataIdentifier inputParam = inputParams.get(i);
	// /*
	// * Note: Using Object is currently preferrable to using
	// * datatype/valuetype to explicitly set the input type to
	// * Integer/Double/Boolean/String since Object allows the automatic
	// * handling of things such as automatic conversions from longs to
	// * ints.
	// */
	// sb.append("Object ");
	// sb.append(inputParam.getName());
	// }
	//
	// sb.append(") {\n");
	// sb.append("String scriptString = \"" + dmlFunctionCall + "\";\n");
	// sb.append(
	// "org.apache.sysml.api.mlcontext.Script script = new
	// org.apache.sysml.api.mlcontext.Script(scriptString);\n");
	//
	// ArrayList<DataIdentifier> outputParams = fs.getOutputParams();
	// if ((inputParams.size() > 0) || (outputParams.size() > 0)) {
	// sb.append("script");
	// }
	// for (int i = 0; i < inputParams.size(); i++) {
	// DataIdentifier inputParam = inputParams.get(i);
	// String name = inputParam.getName();
	// sb.append(".in(\"" + name + "\", " + name + ")");
	// }
	// for (int i = 0; i < outputParams.size(); i++) {
	// DataIdentifier outputParam = outputParams.get(i);
	// String name = outputParam.getName();
	// sb.append(".out(\"" + name + "\")");
	// }
	// if ((inputParams.size() > 0) || (outputParams.size() > 0)) {
	// sb.append(";\n");
	// }
	//
	// sb.append("org.apache.sysml.api.mlcontext.MLResults results =
	// script.execute();\n");
	// sb.append("return results;\n");
	// sb.append("}\n");
	// return sb.toString();
	// }
	//
	// /**
	// * Obtain the DML representing a function invocation.
	// *
	// * @param scriptFilePath
	// * the path to a script file
	// * @param fs
	// * a SystemML function statement
	// * @return string representation of a DML function invocation
	// */
	// public static String generateDmlFunctionCall(String scriptFilePath,
	// FunctionStatement fs) {
	// StringBuilder sb = new StringBuilder();
	// sb.append("source('" + scriptFilePath + "') as mlcontextns;");
	//
	// ArrayList<DataIdentifier> outputParams = fs.getOutputParams();
	// if (outputParams.size() == 0) {
	// sb.append("mlcontextns::");
	// }
	// if (outputParams.size() == 1) {
	// DataIdentifier outputParam = outputParams.get(0);
	// sb.append(outputParam.getName());
	// sb.append(" = mlcontextns::");
	// } else if (outputParams.size() > 1) {
	// sb.append("[");
	// for (int i = 0; i < outputParams.size(); i++) {
	// if (i > 0) {
	// sb.append(", ");
	// }
	// sb.append(outputParams.get(i).getName());
	// }
	// sb.append("] = mlcontextns::");
	// }
	// sb.append(fs.getName());
	// sb.append("(");
	// ArrayList<DataIdentifier> inputParams = fs.getInputParams();
	// for (int i = 0; i < inputParams.size(); i++) {
	// if (i > 0) {
	// sb.append(", ");
	// }
	// DataIdentifier inputParam = inputParams.get(i);
	// sb.append(inputParam.getName());
	// }
	// sb.append(");");
	// return sb.toString();
	// }
	//
	// /**
	// * Obtain the content of a file as a string.
	// *
	// * @param filePath
	// * the path to a file
	// * @return the file content as a string
	// */
	// public static String fileToString(String filePath) {
	// try {
	// File f = new File(filePath);
	// FileReader fr = new FileReader(f);
	// StringBuilder sb = new StringBuilder();
	// int n;
	// char[] charArray = new char[1024];
	// while ((n = fr.read(charArray)) > 0) {
	// sb.append(charArray, 0, n);
	// }
	// fr.close();
	// return sb.toString();
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// return null;
	// }
	//
	// /**
	// * Obtain a constructor body for a Script subclass that sets the
	// * scriptString based on the content of a script file.
	// *
	// * @param scriptFilePath
	// * the path to a script file
	// * @return constructor body for a Script subclass that sets the
	// scriptString
	// * based on the content of a script file
	// */
	// public static String scriptConstructorBody(String scriptFilePath) {
	// StringBuilder sb = new StringBuilder();
	// sb.append("{");
	// sb.append("String scriptFilePath = \"" + scriptFilePath + "\";");
	// sb.append(
	// "java.io.InputStream is =
	// org.apache.sysml.api.mlcontext.Script.class.getResourceAsStream(\"/\"+scriptFilePath);");
	// sb.append("java.io.InputStreamReader isr = new
	// java.io.InputStreamReader(is);");
	// sb.append("int n;");
	// sb.append("char[] charArray = new char[1024];");
	// sb.append("StringBuilder s = new StringBuilder();");
	// sb.append("try {");
	// sb.append(" while ((n = isr.read(charArray)) > 0) {");
	// sb.append(" s.append(charArray, 0, n);");
	// sb.append(" }");
	// sb.append("} catch (java.io.IOException e) {");
	// sb.append(" e.printStackTrace();");
	// sb.append("}");
	// sb.append("setScriptString(s.toString());");
	// sb.append("}");
	// return sb.toString();
	// }

}
