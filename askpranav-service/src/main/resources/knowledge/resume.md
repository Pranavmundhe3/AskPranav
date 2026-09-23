# Resume

Everything in this folder is ingested into the vector store at startup by `KnowledgeFolderLoader`:
`.md`/`.txt` files verbatim, and `.docx`/`.pdf`/`.pptx` via Apache Tika. Drop your own resume here in
any of those formats (a `.docx` or `.pdf` resume is ignored by git on purpose, since it carries contact
details, but the app still reads it locally). Add longer bios or project write-ups the same way and
they are picked up on the next ingestion run.
