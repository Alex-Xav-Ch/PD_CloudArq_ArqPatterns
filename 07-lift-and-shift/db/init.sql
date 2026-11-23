CREATE TABLE students (
    id         SERIAL PRIMARY KEY,
    name       TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO students (name) VALUES ('Ana Torres'), ('Luis Mena'), ('Marta Quispe');
