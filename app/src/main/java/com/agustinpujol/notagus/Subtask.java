package com.agustinpujol.notagus;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// Entidad hija de Task: cada Subtask pertenece a una Task por taskId.
// position sirve para ordenar las subtareas dentro de una tarea.
@Entity(
        tableName = "subtasks",
        foreignKeys = @ForeignKey(
                entity = Task.class,
                parentColumns = "id",
                childColumns = "taskId",
                onDelete = ForeignKey.CASCADE // si se borra la tarea, se borran sus subtareas
        ),
        indices = {
                @Index("taskId") // índice para consultas por taskId
        }
)
public class Subtask {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long taskId;     // FK -> Task.id
    public String title;    // nombre de la subtarea
    public boolean done;    // estado
    public int position;    // orden dentro de la tarea (0,1,2,...)

    // Constructor de conveniencia
    public Subtask(long taskId, String title, boolean done, int position) {
        this.taskId = taskId;
        this.title = title;
        this.done = done;
        this.position = position;
    }
}
