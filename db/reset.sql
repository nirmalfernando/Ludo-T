-- Removes every table. Children first, so foreign keys never block a drop.
DROP TABLE IF EXISTS chat_messages;
DROP TABLE IF EXISTS decisions;
DROP TABLE IF EXISTS seats;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS players;
