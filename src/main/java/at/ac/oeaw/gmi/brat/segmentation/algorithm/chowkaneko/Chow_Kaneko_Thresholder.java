package at.ac.oeaw.gmi.brat.segmentation.algorithm.chowkaneko;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.gui.GenericDialog;
import ij.process.*;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.AbstractList;
import java.util.ArrayList;

/**
 * EN : Compute binarized image from current image by Chow and Kaneko algortihm
 * FR : Calcule une image binaire de l'image courante par la méthode de Chow et
 * Kaneko (source code comments in french, display in english)
 * 
 * @author Vincent Vansuyt
 * @version 1.00
 */
public class Chow_Kaneko_Thresholder implements PlugIn {

	public Chow_Kaneko_Thresholder() {
	}

	/**
	 * Renvoie le nombre de seuils négatifs dans le tableau
	 * 
	 * @param tab_seuils
	 *            : tableau des seuils à deux dimensions
	 * @return nb de seuils négatifs
	 */
	private int compter_nb_seuils_negatifs(int[][] tab_seuils) {
		int nNb_lignes = tab_seuils.length;
		int nNb_colonnes = tab_seuils[0].length;
		int nCpt_nb_seuils_negatifs = 0;
		for (int li = 0; li < nNb_lignes; ++li) {
			for (int co = 0; co < nNb_colonnes; ++co) {
				if (tab_seuils[li][co] < 0) {
					++nCpt_nb_seuils_negatifs;
				}
			}
		}
		return nCpt_nb_seuils_negatifs;
	}// compter_nb_seuils_negatifs

	/**
	 * Remplit le tableau de 8connexite en fonction du tableau des seuils.
	 * 
	 * @param tab_seuils
	 * @param tab_nb_voisins
	 * @return Renvoie le nombre de blocs sans voisins dont le seuil est calcule
	 */
	private int calculer_nb_voisins_8connexite(int[][] tab_seuils,
			int[][] tab_nb_voisins) {
		int nNb_blocs_sans_voisins = 0;
		int nb_blocs_y = tab_seuils.length;
		int nb_blocs_x = tab_seuils[0].length;
		int nNum_by = 0;
		while (nNum_by < nb_blocs_y) {
			// Calcul de la position des voisins
			int y_mu = nNum_by - 1;
			int y_pu = nNum_by + 1;
			if (y_mu < 0) {
				y_mu = -1;
			}
			if (y_pu >= nb_blocs_y) {
				y_pu = -1;
			}
			int nNum_bx = 0;
			while (nNum_bx < nb_blocs_x) {
				// Calcul de la position des voisins
				int x_mu = nNum_bx - 1;
				int x_pu = nNum_bx + 1;
				if (x_mu < 0) {
					x_mu = -1;
				}
				if (x_pu >= nb_blocs_x) {
					x_pu = -1;
				}
				int nNum_connexite = 0;
				// colonne 1
				if (x_mu >= 0) {
					if (y_mu >= 0) {
						if (tab_seuils[y_mu][x_mu] >= 0) {
							nNum_connexite++;
						}
					}
					if (nNum_by >= 0) {
						if (tab_seuils[nNum_by][x_mu] >= 0) {
							nNum_connexite++;
						}
					}
					if (y_pu >= 0) {
						if (tab_seuils[y_pu][x_mu] >= 0) {
							nNum_connexite++;
						}
					}
				}
				// colonne 2
				if (nNum_bx >= 0) {
					if (y_mu >= 0) {
						if (tab_seuils[y_mu][nNum_bx] >= 0) {
							nNum_connexite++;
						}
					}
					if (y_pu >= 0) {
						if (tab_seuils[y_pu][nNum_bx] >= 0) {
							nNum_connexite++;
						}
					}
				}
				// colonne 3
				if (x_pu >= 0) {
					if (y_mu >= 0) {
						if (tab_seuils[y_mu][x_pu] >= 0) {
							nNum_connexite++;
						}
					}
					if (nNum_by >= 0) {
						if (tab_seuils[nNum_by][x_pu] >= 0) {
							nNum_connexite++;
						}
					}
					if (y_pu >= 0) {
						if (tab_seuils[y_pu][x_pu] >= 0) {
							nNum_connexite++;
						}
					}
				}
				// ecriture dans le tableau
				tab_nb_voisins[nNum_by][nNum_bx] = nNum_connexite;
				if (nNum_connexite == 0) {
					nNb_blocs_sans_voisins++;
				}
				// suiv
				nNum_bx++;
			}
			// suiv
			nNum_by++;
		}
		return nNb_blocs_sans_voisins;
	}// calculer_nb_voisins_8connexite

	/**
	 * Calcule le seuil du bloc x, y de la grille à partir des seuils voisins
	 * (en 8connexité) si ils sont positifs )
	 * 
	 * @param tab_seuils
	 *            tableau des seuils
	 * @param nPos_y
	 *            position de la cellule dont on souhaite le seuil
	 * @param nPos_x
	 * @return seuil
	 */
	private int calculer_seuil_grille_a_partir_voisins_8connexite(
			int[][] tab_seuils, int nPos_y, int nPos_x) {
		int nNb_blocs_y = tab_seuils.length;
		int nNb_blocs_x = tab_seuils[0].length;
		int nSeuil = 444;
		// calcul des voisins
		// Y
		int y_mu = nPos_y - 1;
		int y_pu = nPos_y + 1;
		if (y_mu < 0) {
			y_mu = -1;
		}
		if (y_pu >= nNb_blocs_y) {
			y_pu = -1;
		}
		// X
		int x_mu = nPos_x - 1;
		int x_pu = nPos_x + 1;
		if (x_mu < 0) {
			x_mu = -1;
		}
		if (x_pu >= nNb_blocs_x) {
			x_pu = -1;
		}
		// calcul du seuil
		int nSomme_seuils_voisins = 0;
		int nCpt_nb_voisins = 0;
		int ncPoids_coin = 1;
		int ncPoids_4voisin = 1;
		int nSomme_poids = 0;
		// colonne 1
		if (x_mu >= 0) {
			if (y_mu >= 0 && tab_seuils[y_mu][x_mu] >= 0.0) {
				nSomme_seuils_voisins += ncPoids_coin * tab_seuils[y_mu][x_mu];
				nSomme_poids += ncPoids_coin;
			}
			if (nPos_y >= 0 && tab_seuils[nPos_y][x_mu] >= 0.0) {
				nSomme_seuils_voisins += ncPoids_4voisin
						* tab_seuils[nPos_y][x_mu];
				nSomme_poids += ncPoids_4voisin;
			}
			if (y_pu >= 0 && tab_seuils[y_pu][x_mu] >= 0.0) {
				nSomme_seuils_voisins += ncPoids_coin * tab_seuils[y_pu][x_mu];
				nSomme_poids += ncPoids_coin;
			}
		}
		// colonne 2
		if (nPos_x >= 0) {
			if (y_mu >= 0 && tab_seuils[y_mu][nPos_x] > 0.0) {
				nSomme_seuils_voisins += ncPoids_coin
						* tab_seuils[y_mu][nPos_x];
				nSomme_poids += ncPoids_coin;
			}
			if (y_pu >= 0 && tab_seuils[y_pu][nPos_x] > 0.0) {
				nSomme_seuils_voisins += ncPoids_coin
						* tab_seuils[y_pu][nPos_x];
				nSomme_poids += ncPoids_coin;
			}
		}
		// colonne 3
		if (x_pu >= 0) {
			if (y_mu >= 0 && tab_seuils[y_mu][x_pu] > 0.0) {
				nSomme_seuils_voisins += ncPoids_coin * tab_seuils[y_mu][x_pu];
				nSomme_poids += ncPoids_coin;
			}
			if (nPos_y >= 0 && tab_seuils[nPos_y][x_pu] > 0.0) {
				nSomme_seuils_voisins += ncPoids_4voisin
						* tab_seuils[nPos_y][x_pu];
				nSomme_poids += ncPoids_4voisin;
			}
			if (y_pu >= 0 && tab_seuils[y_pu][x_pu] > 0.0) {
				nSomme_seuils_voisins += ncPoids_coin * tab_seuils[y_pu][x_pu];
				nSomme_poids += ncPoids_coin;
			}
		}
		// Renvoie de la moyenne
		if (nSomme_poids > 0) {
			nSeuil = (int) CMaths
					.arrondir(
							((double) nSomme_seuils_voisins / (double) nSomme_poids),
							0);
		} else {
			nSeuil = 255;
		}
		return nSeuil;
	}// calculer_seuil_grille_a_partir_voisins_8connexite

	/**
	 * Calcule l'image des seuils à partir de l'image source
	 * 
	 * @param img_src
	 *            image source
	 * @param img_seuils
	 *            image des seuils
	 * @param x1
	 *            position x coin haut_gauche du ROI
	 * @param y1
	 *            position y coin haut_gauche du ROI
	 * @param ncTaille_bloc
	 *            taille des blocs
	 */
	private void calculer_img_seuils(ImageProcessor img_src,
			ImageProcessor img_seuils, int x1, int y1, int ncTaille_bloc) {
		// AutoThresholder autoThresholder = new AutoThresholder();
		int x, y;
		int nNb_blocs_x = img_seuils.getWidth() / ncTaille_bloc;
		int nNb_blocs_y = img_seuils.getHeight() / ncTaille_bloc;
		// String strM;
		// strM = "nNb_blocs_x = " + nNb_blocs_x + "\n";
		// IJ.log( strM );
		// strM = "nNb_blocs_y = " + nNb_blocs_y + "\n";
		// IJ.log( strM );

		int tab_seuils[][] = new int[nNb_blocs_y][nNb_blocs_x];

		// IJ.log( "Threshold calculation for each bloc\n" );
		int nCpt_seuils_valides = 0;
		boolean bTracer = false;
		int[] vHisto;
		int nNum_by = 0;
		ImageProcessor img_tuile;
		img_tuile = new ByteProcessor(ncTaille_bloc, ncTaille_bloc);
		ImagePlus imp_test = new ImagePlus();
		imp_test.setProcessor(img_tuile);
		while (nNum_by < nNb_blocs_y) {
			// IJ.showProgress( nNum_by , nNb_blocs_y);
			int nY_min = y1 + nNum_by * ncTaille_bloc;
			int nY_max = nY_min + ncTaille_bloc - 1;
			int nNum_bx = 0;
			// String strLigne = "";
			while (nNum_bx < nNb_blocs_x) {
				// Calculer l'histogramme du bloc
				int nX_min = x1 + nNum_bx * ncTaille_bloc;
				int nX_max = nX_min + ncTaille_bloc - 1;
				// bTracer = ( nNum_by == 0 & nNum_bx <= 3 );
				bTracer = false;
				//
				// Copie de la tuile de l'image source
				for (int dy = 0; dy < ncTaille_bloc; dy++) {
					for (int dx = 0; dx < ncTaille_bloc; dx++) {
						int nPix = img_src.getPixel(nX_min + dx, nY_min + dy);
						img_tuile.putPixel(dx, dy, nPix);
					}
				}
				if (bTracer) {
					IJ.showMessage("Debug - debut nNum_by = " + nNum_by
							+ ", nNum_bx = " + nNum_bx);
					imp_test.updateAndDraw();
					imp_test.show("" + nNum_by);
					IJ.showMessage("Debug - nX_min = " + nX_min + " ---fin");
				}
				vHisto = img_tuile.getHistogram();

				// Déterminer si il est bimodal
				bTracer = (nNum_by == 3 && nNum_bx == 1);
				boolean bEst_bimodal = Algos_images.bEst_bimodal(vHisto,
						bTracer);
				int nSeuil = -1;
				if (bEst_bimodal) {
					// nSeuil = autoThresholder.getThreshold(Method.Otsu,
					// vHisto);
					nSeuil = Algos_images.calculer_seuil_Otsu(vHisto);
					++nCpt_seuils_valides;
				}
				tab_seuils[nNum_by][nNum_bx] = nSeuil;
				// strM = String.valueOf( nSeuil );
				// while( strM.length() < 5 ) {
				// strM = ' ' + strM;
				// }
				// strLigne += strM;

				// suivant
				nNum_bx++;
			}
			// IJ.log( strLigne );

			// suivant
			nNum_by++;
		}
		imp_test.close();
		// IJ.log( "Valid thresholds count = " + nCpt_seuils_valides );

		// Propagation des seuils calculés aux seuils non calcules
		// IJ.log("Spreading threshold - debut");

		// Si aucun seuil n'est valide, calcul d'un seuil global et affectation
		// à
		// chaque seuil de bloc
		// IJ.log("Valid thresholds count = " + nCpt_seuils_valides + "/" +
		// (nNb_blocs_x * nNb_blocs_y) + "" );
		if (nCpt_seuils_valides == 0) {
			vHisto = img_src.getHistogram();
			// int nSeuil = img_src.getAutoThreshold( vHisto );
			int nSeuil = Algos_images.calculer_seuil_Otsu(vHisto);
			// IJ.log("Application global threshold : " + nSeuil + "" );
			nNum_by = 0;
			while (nNum_by < nNb_blocs_y) {
				int nNum_bx = 0;
				while (nNum_bx < nNb_blocs_x) {
					tab_seuils[nNum_by][nNum_bx] = nSeuil;
					nNum_bx += 1;
				}
				nNum_by += 1;
			}
		}
		// Création du tableau du nombre de voisins
		// IJ.log( "Creating neighbor table" );
		int tab_nb_voisins[][] = new int[nNb_blocs_y][nNb_blocs_x];

		// Mise à jour du nombre de voisins
		// IJ.log("Count nb non-calculated blocs ");
		int nNb_blocs_non_calcules = compter_nb_seuils_negatifs(tab_seuils);
		int tab_seuils_tmp[][] = new int[nNb_blocs_y][nNb_blocs_x];
		while (nNb_blocs_non_calcules > 0) {
			// IJ.log( "nb non-calculated blocs = " + nNb_blocs_non_calcules +
			// "" );
			int nNb_blocs_sans_voisins = calculer_nb_voisins_8connexite(
					tab_seuils, tab_nb_voisins);
			// Fx_utils.afficher_tableau_entiers_2D( tab_nb_voisins );
			// Calcul des seuils possibles par propagation
			// IJ.log( "nb blocs without neighbors = " + nNb_blocs_sans_voisins
			// + "" );
			// - calculs pour tous les blocs
			int nb_li = tab_seuils.length;
			int nb_co = tab_seuils[0].length;
			int li = 0;
			while (li < nb_li) {
				int co = 0;
				while (co < nb_co) {
					tab_seuils_tmp[li][co] = tab_seuils[li][co];
					if (tab_seuils[li][co] < 0 && tab_nb_voisins[li][co] > 0) {
						tab_seuils_tmp[li][co] = calculer_seuil_grille_a_partir_voisins_8connexite(
								tab_seuils, li, co);
					}
					// suiv
					co += 1;
				}
				// suiv
				li += 1;
			}
			// Copie des résultats issus du tableau de calcul
			li = 0;
			while (li < nb_li) {
				int co = 0;
				while (co < nb_co) {
					tab_seuils[li][co] = tab_seuils_tmp[li][co];
					// suiv
					co++;
				}
				// suiv
				li++;
			}

//			Fx_utils.afficher_tableau_entiers_2D(tab_seuils);
			// suivant
			nNb_blocs_non_calcules = compter_nb_seuils_negatifs(tab_seuils);
		}
		// Calcul de l'interpolation
		interpoler_grille_sur_ImageProcessor(img_seuils, tab_seuils,
				ncTaille_bloc);
	}// calculer_img_seuils

	/**
	 * Interpole le tableau des seuils sur l'image reçue
	 * 
	 * @param img_seuils
	 *            l'image des seuils à peupler
	 * @param tab_seuils
	 *            le tableau des seuils
	 * @param ncTaille_bloc
	 *            la taille des blocs en pixels( peut-être redondant)
	 */
	private void interpoler_grille_sur_ImageProcessor(
			ImageProcessor img_seuils, int[][] tab_seuils, int ncTaille_bloc) {
		boolean bTracer = false;
		if (bTracer)
			IJ.log("interpoler_grille_sur_calque - debut");
		int ncTaille_demi_bloc = (int) CMaths
				.arrondir((double) ncTaille_bloc / 2.0);

		// pour chaque ligne
		int nb_lignes = tab_seuils.length;
		int nb_colonnes = tab_seuils[0].length;
		int li = 0;
		AbstractList tab_pts_ligne;
		tab_pts_ligne = new ArrayList();
		while (li < nb_lignes) {
			// bTracer = ( li == 0 );
			if (bTracer)
				IJ.log("Ligne " + li + "");
			if (bTracer)
				IJ.log("--------------------");
			int y_ligne = li * ncTaille_bloc + (ncTaille_bloc / 2); // position
																	// de la
																	// ligne a
																	// partir du
																	// haut de l
																	// image
			// + calcul des points extremes
			// - + réservation et remplissage de l'espace

			// tab_pts_ligne = [Point(0,0)]*(nb_colonnes + 2 );
			tab_pts_ligne.clear();
			for (int i = 0; i < (nb_colonnes + 2); ++i) {
				tab_pts_ligne.add(new Point2D.Double(0, 0));
			}
			// tab_pentes_ligne = [0.0]*(nb_colonnes + 2 )
			double[] tab_pentes_ligne = new double[nb_colonnes + 2];
			int n = 0;
			while (n < (nb_colonnes + 2)) {
				double nAbscisse = (double) (n * ncTaille_bloc)
						- ncTaille_demi_bloc;
				if (n == 0) {
					tab_pts_ligne.set(n, new Point2D.Double(0.0, 0.0));
				} else if (n == (nb_colonnes + 2 - 1)) {
					nAbscisse = (double) (img_seuils.getWidth() - 1);
					tab_pts_ligne.set(n, new Point2D.Double(nAbscisse, 0));
				} else {
					tab_pts_ligne.set(n, new Point2D.Double(nAbscisse,
							(double) (tab_seuils[li][n - 1])));
				}
				// suiv
				n++;
			}// while

			// self.afficher_liste_points_et_pentes( tab_pts_ligne,
			// tab_pentes_ligne, fLog ) // affichage avant calculs

			// + calcul des pentes pour tous les points sauf le premier et
			// dernier
			if (bTracer)
				IJ.log("Calcul pentes");
			int nb_pts = tab_pts_ligne.size();
			int i = 2;
			Point2D.Double vAC = new Point2D.Double(0, 0);
			Point2D.Double vAB = new Point2D.Double(0, 0);
			while (i < (nb_pts - 1 - 1)) {
				vAC.x = ((Point2D.Double) (tab_pts_ligne.get(i + 1))).x
						- ((Point2D.Double) (tab_pts_ligne.get(i - 1))).x;
				vAC.y = ((Point2D.Double) (tab_pts_ligne.get(i + 1))).y
						- ((Point2D.Double) (tab_pts_ligne.get(i - 1))).y;
				tab_pentes_ligne[i] = vAC.y / vAC.x;
				// suiv
				i++;
			}
			// +- deux premières pentes
			vAB.x = ((Point2D.Double) (tab_pts_ligne.get(2))).x
					- ((Point2D.Double) (tab_pts_ligne.get(1))).x;
			vAB.y = ((Point2D.Double) (tab_pts_ligne.get(2))).y
					- ((Point2D.Double) (tab_pts_ligne.get(1))).y;
			tab_pentes_ligne[0] = vAB.y / vAB.x;
			tab_pentes_ligne[1] = vAB.y / vAB.x;
			// +- deux dernières pentes
			vAB.x = ((Point2D.Double) (tab_pts_ligne.get(nb_pts - 2))).x
					- ((Point2D.Double) (tab_pts_ligne.get(nb_pts - 3))).x;
			vAB.y = ((Point2D.Double) (tab_pts_ligne.get(nb_pts - 2))).y
					- ((Point2D.Double) (tab_pts_ligne.get(nb_pts - 3))).y;
			tab_pentes_ligne[nb_pts - 2] = vAB.y / vAB.x;
			tab_pentes_ligne[nb_pts - 1] = vAB.y / vAB.x;

			// + calcul des ordonnees des premiers et derniers points
			if (bTracer)
				IJ.log("Calcul ordonnee premier et dernier");
			// +- premier pt
			double dx = ((Point2D.Double) tab_pts_ligne.get(1)).x
					- ((Point2D.Double) tab_pts_ligne.get(0)).x;
			((Point2D.Double) (tab_pts_ligne.get(0))).y = ((Point2D.Double) tab_pts_ligne
					.get(1)).y - dx * tab_pentes_ligne[1];
			// +- dernier pt
			dx = ((Point2D.Double) tab_pts_ligne.get(nb_pts - 1)).x
					- ((Point2D.Double) tab_pts_ligne.get(nb_pts - 2)).x;
			((Point2D.Double) tab_pts_ligne.get(nb_pts - 1)).y = ((Point2D.Double) tab_pts_ligne
					.get(nb_pts - 2)).y + dx * tab_pentes_ligne[nb_pts - 2];

			// + calcul des pentes en chaque point
			// + calcul des valeurs en chaque point grace à la fonction
			// d'Hermite
			if (bTracer)
				IJ.log("Interpolation Hermite");
			CMaths maths = new CMaths();
			AbstractList vPts = maths.getPoly_Hermite(tab_pts_ligne, 1);
			// AbstractList vPts = maths.getSpline_naturelle( tab_pts_ligne, 1
			// );
			nb_pts = vPts.size();
			i = 0;
			while (i < nb_pts) {
				int x_pix = (int) CMaths
						.arrondir(((Point2D.Double) vPts.get(i)).x);
				// int pos = ( int( x_pix ) + nLargeur * y_ligne ) * p_size
				int y_pix = y_ligne;
				int cl_pix = (int) CMaths.arrondir(((Point2D.Double) vPts
						.get(i)).y);
				if (cl_pix > 255) {
					cl_pix = 255;
				}
				if (cl_pix < 0) {
					cl_pix = 0;
				}
				img_seuils.putPixel(x_pix, y_pix, cl_pix);

				// suiv
				i++;
			}
			if (bTracer) {
				IJ.log("Interpolation ligne " + li);
				n = 0;
				while (n < nb_colonnes + 2) {
					IJ.log("" + n + " "
							+ ((Point2D.Double) tab_pts_ligne.get(n)).x + " "
							+ ((Point2D.Double) tab_pts_ligne.get(n)).y + " "
							+ tab_pentes_ligne[n]);
					// suivant
					++n;
				}
			}

			// ligne suivante
			++li;
		}// while ( li < nb_lignes) {

		// ----------------------------
		// pour chaque colonne...
		// ----------------------------
		if (bTracer)
			IJ.log("-----------------------------------");
		if (bTracer)
			IJ.log("Colonnes");
		int x_colonne = 0;
		// x_colonne sur calque_seuils.width
		while (x_colonne < img_seuils.getWidth()) {// img_seuils.getWidth() ) {
			// + recuperation de la liste des valeurs a interpoler sur l'image
			AbstractList tab_pts_colonne;
			tab_pts_colonne = new ArrayList();
			tab_pts_colonne.add(new Point2D.Double(0.0, 0.0));// premier point
			li = 0;
			while (li < nb_lignes) {
				int y_ligne = li * ncTaille_bloc + (ncTaille_bloc / 2);// #
																		// position
																		// de la
																		// ligne
																		// a
																		// partir
																		// du
																		// haut
																		// de l
																		// image
				int cl_pix = img_seuils.get(x_colonne, y_ligne);
				tab_pts_colonne.add(new Point2D.Double((double) y_ligne,
						(double) cl_pix));
				// suiv
				++li;
			}
			tab_pts_colonne.add(new Point2D.Double(img_seuils.getHeight() - 1,
					0.0));// dernier point

			// + calcul des points extremes (première et derniere ligne) à
			// partir des pentes
			if (bTracer)
				IJ.log("Calcul des points extremes");
			double f_cl_pix;
			// -+ premier point
			Point2D.Double vAB = new Point2D.Double(0, 0);
			Point2D.Double ptA = (Point2D.Double) tab_pts_colonne.get(1);
			Point2D.Double ptB = (Point2D.Double) tab_pts_colonne.get(2);
			vAB.x = ptB.x - ptA.x;
			vAB.y = ptB.y - ptA.y;
			double pente = vAB.y / vAB.x;
			double dx = ptA.x - 0.0;
			f_cl_pix = ptA.y - dx * pente;
			tab_pts_colonne.set(0, new Point2D.Double(0.0, f_cl_pix));
			img_seuils.putPixel(x_colonne, 0, CMaths.arrondir(f_cl_pix));
			//
			// -+ Dernier point
			int nb_points = tab_pts_colonne.size();
			vAB.x = ((Point2D.Double) tab_pts_colonne.get(nb_points - 2)).x
					- ((Point2D.Double) tab_pts_colonne.get(nb_points - 3)).x;
			vAB.y = ((Point2D.Double) tab_pts_colonne.get(nb_points - 2)).y
					- ((Point2D.Double) tab_pts_colonne.get(nb_points - 3)).y;
			pente = vAB.y / vAB.x;
			dx = (double) (img_seuils.getHeight() - 1)
					- ((Point2D.Double) tab_pts_colonne.get(nb_points - 2)).x;
			f_cl_pix = (double) ((Point2D.Double) tab_pts_colonne
					.get(nb_points - 2)).y + dx * pente;
			Point2D.Double ptAjout = new Point2D.Double(
					(double) (img_seuils.getHeight() - 1), f_cl_pix);
			tab_pts_colonne.set(nb_points - 1, ptAjout);
			img_seuils.putPixel(x_colonne, img_seuils.getHeight() - 1,
					CMaths.arrondir(f_cl_pix));

			/*
			 * if ( x_colonne == 0 ) {
			 * IJ.log("Debug interpolation colonne 0 - debut"); for ( int i = 0;
			 * i < tab_pts_colonne.size(); ++i ) { Point2D.Double ptTmp =
			 * (Point2D.Double)tab_pts_colonne.get(i); IJ.log("" + i + ", x = "
			 * + ptTmp.x + ", y = " + ptTmp.y ); }
			 * IJ.log("Debug interpolation colonne 0 - fin"); }
			 */

			// + calcul des valeurs en chaque point grace à la fonction
			// d'Hermite

			AbstractList vPts = CMaths.getPoly_Hermite(tab_pts_colonne, 1);
			// AbstractList vPts = CMaths.getSpline_naturelle( tab_pts_colonne,
			// 1 );
			int nb_pts = vPts.size();
			int cl_pix;
			int i = 0;
			while (i < nb_pts) {
				int y_pix = (int) ((Point2D.Double) vPts.get(i)).x;
				cl_pix = (int) ((Point2D.Double) vPts.get(i)).y;
				if (cl_pix > 255) {
					cl_pix = 255;
				}
				if (cl_pix < 0) {
					cl_pix = 0;
				}
				img_seuils.putPixel(x_colonne, y_pix, cl_pix);
				// suiv
				++i;
			}
			if (bTracer)
				IJ.log("Interpolation Hermite - fin");

			// colonne suivante
			++x_colonne;
		}

		if (bTracer)
			IJ.log("interpoler_grille_sur_calque - fin");
	}// interpoler_grille_sur_ImageProcessor

	/**
	 * Binarise l'image recue
	 * 
	 * @param imp
	 *            image en niveau de gris à binariser
	 * @param nTaille_bloc
	 *            taille de blocs en pixels
	 */
	private void binariser(ImagePlus imp, int nTaille_bloc) {
		ImageProcessor img = imp.getProcessor();
		if (img == null) {
			IJ.showMessage("Warning", "You didn't choose any picture");
			return;
		}
		// Récupération de la sélection
		Rectangle rectROI = img.getRoi();
		IJ.log("rectROI = " + rectROI);

		// Ajout d'un calque pour l'image des seuils du même type que l'image
		int nType = imp.getType();
		String strType;
		if (nType == ImagePlus.GRAY8) {
			strType = "8 bits White";
		} else {
			strType = "16 bits White";
		}
		final int ncDepth_img_seuils = 1;
		ImagePlus imp_seuils = IJ.createImage("Thresholds map", strType,
				rectROI.width, rectROI.height, ncDepth_img_seuils);
		ImageProcessor img_seuils = imp_seuils.getProcessor();
		calculer_img_seuils(img, img_seuils, rectROI.x, rectROI.y, nTaille_bloc);
		imp_seuils.setProcessor(img_seuils);
		imp_seuils.updateAndDraw();
		imp_seuils.show();

		imp.setProcessor(img);
		imp.updateAndDraw();
		imp.show();

		final int nDepth = 1;
		ImagePlus imp_binarisee = IJ.createImage("Binarized picture",
				"8-bit White", img_seuils.getWidth(), img_seuils.getHeight(),
				nDepth);
		ImageProcessor img_binarisee = imp_binarisee.getProcessor();
		// Application des seuils pour chaque pixel dans un troisieme calque
		IJ.log("Threshold application");
		int x1 = rectROI.x;
		int y1 = rectROI.y;
		int x2 = x1 + rectROI.width;
		int y2 = y1 + rectROI.height;
		int y = 0;
		int nCpt_y = (y2 - y1);
		while (y < nCpt_y) {
			IJ.showProgress(y, nCpt_y);
			int x = 0;
			while (x < (x2 - x1)) {
				int x_img_src = x1 + x;
				int y_img_src = y1 + y;
				int cl_img_src = img.getPixel(x_img_src, y_img_src);
				int x_img_seuils = x;
				int y_img_seuils = y;
				int cl_img_seuil = img_seuils.getPixel(x_img_seuils,
						y_img_seuils);
				int v = 0;
				if (cl_img_src > cl_img_seuil) {
					v = 255;
				}
				img_binarisee.putPixel(x_img_seuils, y_img_seuils, v);
				// suiv
				++x;
			}
			// suiv
			++y;
		}// while

		imp_binarisee.updateAndDraw();
		imp_binarisee.show();
	}// binariser

	public ImageProcessor binarizeIp(ImageProcessor srcIp, int blkSize,int thresOffset) {
		// Récupération de la sélection
		Rectangle rectROI = srcIp.getRoi();

		// // Ajout d'un calque pour l'image des seuils du même type que l'image
		// int nType = imp.getType();
		// String strType;
		// if ( nType == ImagePlus.GRAY8 ) {
		// strType = "8 bits White";
		// }
		// else {
		// strType = "16 bits White";
		// }
		// final int ncDepth_img_seuils = 1;
		// ImagePlus imp_seuils = IJ.createImage( "Thresholds map",
		// strType,
		// rectROI.width, rectROI.height,
		// ncDepth_img_seuils );
		ImageProcessor img_seuils = srcIp.createProcessor(rectROI.width,
				rectROI.height);
		calculer_img_seuils(srcIp, img_seuils, rectROI.x, rectROI.y, blkSize);
//		new ImagePlus("chow kaneko thres",img_seuils).show();
		// final int nDepth = 1;
		// ImagePlus imp_binarisee = IJ.createImage("Binarized picture",
		// "8-bit White",
		// img_seuils.getWidth(),
		// img_seuils.getHeight(),
		// nDepth );
		ImageProcessor img_binarisee = srcIp.createProcessor(rectROI.width,
				rectROI.height);
		// Application des seuils pour chaque pixel dans un troisieme calque
		// IJ.log( "Threshold application" );
		int x1 = rectROI.x;
		int y1 = rectROI.y;
		int x2 = x1 + rectROI.width;
		int y2 = y1 + rectROI.height;
		int y = 0;
		int nCpt_y = (y2 - y1);
		while (y < nCpt_y) {
			// IJ.showProgress(y, nCpt_y );
			int x = 0;
			while (x < (x2 - x1)) {
				int x_img_src = x1 + x;
				int y_img_src = y1 + y;
				int cl_img_src = srcIp.getPixel(x_img_src, y_img_src);
				int x_img_seuils = x;
				int y_img_seuils = y;
				int cl_img_seuil = img_seuils.getPixel(x_img_seuils,
						y_img_seuils);
				int v = 0;
				if (cl_img_src > cl_img_seuil+thresOffset) {
					v = 255;
				}
				img_binarisee.putPixel(x_img_seuils, y_img_seuils, v);
				// suiv
				++x;
			}
			// suiv
			++y;
		}// while

		// imp_binarisee.updateAndDraw();
		// imp_binarisee.show();
		return img_binarisee;
	}

	/**
	 * Point d'entree du plugin
	 * 
	 * @param string
	 */
	@Override
	public void run(String string) {
		IJ.log("\\Clear");
		IJ.log("------- Chow and Kaneko binarization - begin ------");
		// Vérification du type d'image
		ImagePlus imp = IJ.getImage();
		int nType = imp.getType();
		if (nType != ImagePlus.GRAY8 && nType != ImagePlus.GRAY16) {
			IJ.showMessage("Warning", "The image must in gray scale");
			IJ.log("Wrong type : The image must be in grayscale");
			return;
		}

		// Saisie de la taille d'une tuile
		int nTaille_bloc = 32;
		GenericDialog gd = new GenericDialog("Binarization");
		gd.addMessage("Binarization using Chow and Kaneko");
		final int ncNb_decimales = 0;
		gd.addNumericField("Bloc size (pixels) : ", nTaille_bloc,
				ncNb_decimales);
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.log("Annulation");
			return;
		}
		nTaille_bloc = (int) gd.getNextNumber();
		IJ.log("Bloc size (pixels) : " + nTaille_bloc);

		long t_debut_millisec = System.currentTimeMillis();
		binariser(imp, nTaille_bloc);
		long t_fin_millisec = System.currentTimeMillis();

		IJ.log("------- Chow and Kaneko binarization de  - fin ------");
		long t_duree_millisec = t_fin_millisec - t_debut_millisec;
		IJ.log("Ellapsed time : "
				+ CMaths.arrondir((double) t_duree_millisec / 1000.0, 1)
				+ " seconds");
	}// run
}