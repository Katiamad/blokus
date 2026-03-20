package fr.orleans.m1miage.project.s2.projetS2.model;

import java.util.ArrayList;
import java.util.List;

public class PieceFactory {
    public static List<Piece> creerToutesLesPieces() {
        List<Piece> pieces = new ArrayList<>();

        // 1. Monomino (1 carré)
        pieces.add(new Piece(1, "Any", new int[][]{{1}},1));

        // 2. Domino (2 carrés)
        pieces.add(new Piece(2, "Any", new int[][]{{1, 1}},2));

        // 3. Tromino I (3 carrés en ligne)
        pieces.add(new Piece(3, "Any", new int[][]{{1, 1, 1}},3));

        // 4. Tromino L
        pieces.add(new Piece(4, "Any", new int[][]{
                {1, 0},
                {1, 1}
        },3));

        // 5. Tetromino I (4 carrés en ligne)
        pieces.add(new Piece(5, "Any", new int[][]{{1, 1, 1, 1}},4));

        // 6. Tetromino L
        pieces.add(new Piece(6, "Any", new int[][]{
                {0, 1},
                {0, 1},
                {1, 1}
        },4));

        // 7. Tetromino T
        pieces.add(new Piece(7, "Any", new int[][]{
                {1, 1, 1},
                {0, 1, 0}
        },4));

        // 8. Tetromino O (carré 2x2)
        pieces.add(new Piece(8, "Any", new int[][]{
                {1, 1},
                {1, 1}
        },4));

        // 9. Tetromino S
        pieces.add(new Piece(9, "Any", new int[][]{
                {0, 1, 1},
                {1, 1, 0}
        },4));

        // 10. Pentomino I (ligne de 5 carrés)
        pieces.add(new Piece(10, "Any", new int[][]{
                {1, 1, 1, 1, 1}
        },5));

        // 11.
        pieces.add(new Piece(11, "Any", new int[][]{
                {1, 1, 1, 1},
                {0, 0, 0, 1}
        },5));

        // 12. Pentomino N
        pieces.add(new Piece(12, "Any", new int[][]{
                {1, 1, 0, 0},
                {0, 1, 1, 1}
        },5));

        // 13. Pentomino P
        pieces.add(new Piece(13, "Any", new int[][]{
                {1, 1},
                {1, 1},
                {1, 0}
        },5));

        // 14. Pentomino U
        pieces.add(new Piece(14, "Any", new int[][]{
                {1, 0, 1},
                {1, 1, 1}
        },5));

        // 15. Pentomino Y
        pieces.add(new Piece(15, "Any", new int[][]{
                {0, 1},
                {1, 1},
                {0, 1},
                {0, 1}
        },5));

        // 16.
        pieces.add(new Piece(16, "Any", new int[][]{
                {0, 1, 0},
                {0, 1, 0},
                {1, 1, 1}
        },5));

        // 17. Pentomino V
        pieces.add(new Piece(17, "Any", new int[][]{
                {1, 0, 0},
                {1, 0, 0},
                {1, 1, 1}
        },5));

        // 18. Pentomino W
        pieces.add(new Piece(18, "Any", new int[][]{
                {1, 0, 0},
                {1, 1, 0},
                {0, 1, 1}
        },5));


        // 19.
        pieces.add(new Piece(19, "Any", new int[][]{
                {1, 0, 0},
                {1, 1, 1},
                {0, 0, 1}
        },5));

        // 20.
        pieces.add(new Piece(20, "Any", new int[][]{
                {1, 0, 0},
                {1, 1, 1},
                {0, 1, 0}
        },5));

        // 21. Pentomino X
        pieces.add(new Piece(21, "Any", new int[][]{
                {0, 1, 0},
                {1, 1, 1},
                {0, 1, 0}
        },5));

        return pieces;
    }
}