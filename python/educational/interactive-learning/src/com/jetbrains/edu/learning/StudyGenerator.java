package com.jetbrains.edu.learning;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.StudyNames;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class StudyGenerator {
  private StudyGenerator() {

  }
  private static final Logger LOG = Logger.getInstance(StudyGenerator.class.getName());

  /**
   * Creates task files in its task folder in project user created
   *
   * @param taskDir      project directory of task which task file belongs to
   * @param resourceRoot directory where original task file stored
   * @throws IOException
   */
  public static void createTaskFile(@NotNull final VirtualFile taskDir, @NotNull final File resourceRoot,
                                    @NotNull final String name) throws IOException {
    String systemIndependentName = FileUtil.toSystemIndependentName(name);
    final int index = systemIndependentName.lastIndexOf("/");
    if (index > 0) {
      systemIndependentName = systemIndependentName.substring(index + 1);
    }
    File resourceFile = new File(resourceRoot, name);
    File fileInProject = new File(taskDir.getPath(), systemIndependentName);
    FileUtil.copy(resourceFile, fileInProject);
  }

  /**
   * Creates task directory in its lesson folder in project user created
   *
   * @param lessonDir    project directory of lesson which task belongs to
   * @param resourceRoot directory where original task file stored
   * @throws IOException
   */
  public static void createTask(@NotNull final Task task, @NotNull final VirtualFile lessonDir, @NotNull final File resourceRoot,
                                @NotNull final Project project) throws IOException {
    VirtualFile taskDir = lessonDir.createChildDirectory(project, StudyNames.TASK_DIR + Integer.toString(task.getIndex() + 1));
    StudyUtils.markDirAsSourceRoot(taskDir, project);
    File newResourceRoot = new File(resourceRoot, taskDir.getName());
    int i = 0;
    for (Map.Entry<String, TaskFile> taskFile : task.getTaskFiles().entrySet()) {
      TaskFile taskFileContent = taskFile.getValue();
      taskFileContent.setIndex(i);
      i++;
      createTaskFile(taskDir, newResourceRoot, taskFile.getKey());
    }
    File[] filesInTask = newResourceRoot.listFiles();
    if (filesInTask != null) {
      for (File file : filesInTask) {
        String fileName = file.getName();
        if (!task.isTaskFile(fileName)) {
          File resourceFile = new File(newResourceRoot, fileName);
          File fileInProject = new File(taskDir.getCanonicalPath(), fileName);
          FileUtil.copy(resourceFile, fileInProject);
        }
      }
    }
  }

  /**
   * Creates lesson directory in its course folder in project user created
   *
   * @param courseDir    project directory of course
   * @param resourceRoot directory where original lesson stored
   * @throws IOException
   */
  public static void createLesson(@NotNull final Lesson lesson, @NotNull final VirtualFile courseDir, @NotNull final File resourceRoot,
                                  @NotNull final Project project) throws IOException {
    String lessonDirName = StudyNames.LESSON_DIR + Integer.toString(lesson.getIndex() + 1);
    VirtualFile lessonDir = courseDir.createChildDirectory(project, lessonDirName);
    final List<Task> taskList = lesson.getTaskList();
    for (int i = 0; i < taskList.size(); i++) {
      Task task = taskList.get(i);
      task.setIndex(i);
      createTask(task, lessonDir, new File(resourceRoot, lessonDir.getName()), project);
    }
  }

  /**
   * Creates course directory in project user created
   *
   * @param baseDir      project directory
   * @param resourceRoot directory where original course is stored
   */
  public static void createCourse(@NotNull final Course course, @NotNull final VirtualFile baseDir, @NotNull final File resourceRoot,
                                  @NotNull final Project project) {
    ApplicationManager.getApplication().invokeLater(
      new Runnable() {
        @Override
        public void run() {
          ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
              try {
                final List<Lesson> lessons = course.getLessons();
                for (int i = 0; i < lessons.size(); i++) {
                  Lesson lesson = lessons.get(i);
                  lesson.setIndex(i);
                  createLesson(lesson, baseDir, resourceRoot, project);
                }
                baseDir.createChildDirectory(this, StudyNames.SANDBOX_DIR);
                File[] files = resourceRoot.listFiles(new FilenameFilter() {
                  @Override
                  public boolean accept(File dir, String name) {
                    return !name.contains(StudyNames.LESSON_DIR) && !name.equals("course.json") && !name.equals("hints");
                  }
                });
                for (File file : files) {
                  FileUtil.copy(file, new File(baseDir.getPath(), file.getName()));
                }
              }
              catch (IOException e) {
                LOG.error(e);
              }
            }
          });
        }
      });
  }
}
