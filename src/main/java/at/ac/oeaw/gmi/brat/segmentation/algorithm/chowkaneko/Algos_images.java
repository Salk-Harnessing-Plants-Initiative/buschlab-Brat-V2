package at.ac.oeaw.gmi.brat.segmentation.algorithm.chowkaneko;

import ij.IJ;
import java.awt.geom.Point2D;
import java.awt.Point;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Vector;
import java.io.*;
import java.math.*;

/**
 * Algorithmes sur les images et leurs histogrammes
 * @author VV
 */
public class Algos_images {
	
	/**
	 * Calcul du seuil par maximisation de la variance inter-classes (Otsu)
	 * @param vHisto_pix
	 * @return le seuil d'Otsu
	 */
	public static int calculer_seuil_Otsu( int[] vHisto ) {
		double v = 0.0;
		double vm = 0.0;
		int s = 0;
		int sm_premier = 0;
		int sm_dernier = 0;
		int nTaille_histo = vHisto.length;

		double m = 0.0;
		double p = 0.0;
		int i = 0;
		while( i < nTaille_histo ) {
			p += (double)vHisto[i];
			m += (double)( i * vHisto[i] );
			++i;
		}
		
		if ( p != 0.0 ) {
			m /= p;
		}

		s = 0;
		while ( s < nTaille_histo ) {
			// Calcul sur la partie gauche de l'histogramme
			double p1 = 0.0;
			double m1 = 0.0;
			i = 0;
			while( i <= s ) {
				p1 += (double)vHisto[i];
				m1 += (double)( i * vHisto[i] );
				++i;
			}

			if ( p1 != 0.0 ) {
				m1 /= p1;
			}

			// Calcul sur la partie droite de l'histogramme
			double p2 = 0.0;
			double m2 = 0.0;
			i = s + 1;
			while( i < nTaille_histo ) {
				p2 += (double)vHisto[i];
				m2 += (double)( i * vHisto[i] );
				++i;
			}

			if ( p2 != 0.0 ) {
				m2 /= p2;
			}

			// Calcul et enregistrement de la variance max
			v = p1 * (m1 - m)*(m1 - m) + p2 * (m2 - m)*(m2 - m);
			if ( v >= vm && m1 > 0.0 && m2 > 0.0) {
				sm_premier = s;
				if ( v > vm ) {
					sm_dernier = s;
					vm = v;
				}
			}
			// suiv
			++s;
		}
		return (int)Math.floor((sm_premier + sm_dernier ) / 2);
	}// calculer_seuil_Otsu
	
	
	/**
	 * Vectorise l'histogramme reçu
	 * @param vHisto_pix
	 * @param ncSeuil_ajout_carre
	 * @return une "List" de java.awt.Point
	 */
	public static AbstractList vectoriser_histogramme( double[] vHisto_pix, 
			                                    double ncSeuil_ajout_carre,
												boolean bTracer ) {
		//Vectorise l'histogramme en un polygone grâce à l'algorithme de la corde
		AbstractList vPoly_histo;
		vPoly_histo = new ArrayList();

		// Ajout des extrémités
		// self.ecrire_histogramme( vHisto_pix, fLog )
		int nTaille_histo = vHisto_pix.length;
		vPoly_histo.add( new Point.Double( 0, vHisto_pix[0]) );
		vPoly_histo.add( new Point.Double( nTaille_histo - 1,  vHisto_pix[ nTaille_histo - 1 ]) );

		// Recherche d'un point à insérer entre chaque segment
		//ptAjout = Point()
		Point.Double ptAjout = new Point.Double( 0, 0 );
		boolean bAjout_point = true;
		while ( bAjout_point ) {
			bAjout_point = false;
			int i = 0;
			while ( i < ( vPoly_histo.size() - 1 ) ) {
				Point.Double ptA = (Point.Double)vPoly_histo.get( i );
				++i;
				Point.Double ptB = (Point.Double)vPoly_histo.get( i );
				Point.Double vAB = new Point.Double();// = ptB - ptA;
				vAB.setLocation( ptB.x - ptA.x, ptB.y - ptA.y );
				double nLg_AB2 = vAB.x * vAB.x + vAB.y * vAB.y;
				double nLg_ajout = ncSeuil_ajout_carre;
				int iAjout = -1;
				// recherche le point le plus éloigné du segment AB vectorisé dans 
				//  l'histogramme pixelise
				int j = (int)ptA.x + 1;
				while ( j < ptB.x ) {
					Point.Double ptC = new Point.Double( j, vHisto_pix[ j ] );
					// calcul de la distance de ptC au segment ptA - ptB
					Point.Double vAC = new Point.Double();
					vAC.x = ptC.x - ptA.x;
					vAC.y = ptC.y - ptA.y;
					Double t = (Double)( vAB.x * vAC.x + vAB.y * vAC.y ) / nLg_AB2;
					if ( t >= 0.0 && t <= 1.0 ) {
						Point.Double ptI = new Point.Double( (double)(ptA.x + t * vAB.x + 0.5), 
								                             (double)(ptA.y + t * vAB.y + 0.5));
						Point.Double vCI = new Point.Double();
						vCI.x = ptI.x - ptC.x;
						vCI.y = ptI.y - ptC.y;
						double nLg_CI2 = vCI.x * vCI.x + vCI.y * vCI.y;
						if ( nLg_CI2 > nLg_ajout ) {
							// Mémorisation
							bAjout_point = true;
							nLg_ajout = nLg_CI2;
							iAjout = i;
							ptAjout = new Point.Double( ptC.x, ptC.y );
						}
					}
					// suiv
					++j;
				}
				// Ajout éventuel
				if ( bAjout_point && iAjout > 0) {
					vPoly_histo.add( iAjout, ptAjout);
					++i;
				}
			}// while j
		}// while bAjout_point
		
		// Affichage des points de l'histogramme vectorise
		if ( bTracer ) {
			try {
				PrintWriter fOut = new PrintWriter( 
										new BufferedWriter( 
											new FileWriter("log_histogramme_vectorise.txt")));
		
				int i = 0;
				while ( i < vPoly_histo.size() ) {
					Point.Double pt = (Point.Double)vPoly_histo.get( i );
					fOut.println( pt.x + " " + pt.y );
					// suiv
					i += 1;
				}
				fOut.close();
			}
			catch ( IOException e ) {
				IJ.log( "Erreur creation fichier" );
			}
		}
		return vPoly_histo;
	}// vectoriser_histogramme
	
	
	/**
	 * 
	 * @param vHisto histogramme (tableau de 256 entiers)
	 * @param winSize fenêtre de lissage (de taille 6 par défaut)
	 * @return histogramme lissé (tableau de 256 entiers)
	 */
	static public double[] lisser_histogramme( double[] vHisto, int winSize ) {
		
		int winMidSize = winSize / 2;

		int nTaille = vHisto.length;
		// Copie de l'histogramme actuel dans le tableau temporaire
		double[] vHisto_copie = (double[])vHisto.clone();
		
		// Calcul de la moyenne dans la copie
		int i = 0;
		while (  i < nTaille ) {
			double mean = 0.0;
			int j = i - winMidSize;
			while( j <= (i + winMidSize) ) {
				if ( j >= 0 && j <  nTaille ){ // dans le tableau 
					mean += vHisto[ j ];
				}
				j = j + 1;
			}
			vHisto_copie[ i ] = mean / (double)( winSize + 1);
			i = i + 1;
		}
		
		return vHisto_copie;
	}// lisser_histogramme
	
	
	/**
	 * Renvoie le nombre de maximums du polygone de l'histogramme (de la gauche vers
	 *  la droite)
	 *  Renvoie le tableau des points max
	 * @param vHisto liste de java.awt.Point
	 * @param vPts_max liste de java.awt.Point
	 * @param ptMin java.awt.Point
	 * @return Renvoie le tableau des points max
	 */
	static public int getNb_maximums_histogramme_vectorise( AbstractList vHisto, 
			                                                AbstractList vPts_max, 
															Point.Double[] ptMin,
															boolean bTracer ) {
		int nNb_maximums = 0;
		int nTaille = vHisto.size();
		if ( nTaille < 3 ) {
			return nNb_maximums;
		}
		int nX_debut = -1;
		int nX_fin = -1;
		Point.Double ptA = new Point.Double();
		Point.Double ptB = new Point.Double();
		Point.Double ptC = new Point.Double();
		int i = 0;
		while ( i < ( vHisto.size() - 2 ) ) {
			ptA = (Point.Double)vHisto.get( i );
			ptB = (Point.Double)vHisto.get( i + 1 );
			ptC = (Point.Double)vHisto.get( i + 2 );
			double nPente_AB = (double)( ptB.y - ptA.y ) / (double)( ptB.x - ptA.x );
			double nPente_BC = (double)( ptC.y - ptB.y ) / (double)( ptC.x - ptB.x );
			if ( nPente_AB > 0 && nPente_BC <= 0 ) {
				nX_debut = (int)ptB.x;
			}
			if ( nPente_AB >= 0 && nPente_BC < 0 ) {
				nX_fin = (int)ptB.x;
			}
			if (( nX_debut >= 0 && nX_fin >= 0 ) || 
				( nPente_AB < 0 && i == 0 ) ||
				( nPente_BC > 0 && i == ( vHisto.size()  - 3 ) ) ) { 
				nNb_maximums += 1;
				float nX_maximum = ( nX_debut + nX_fin ) / 2;
				vPts_max.add( new Point.Double( nX_maximum, ptB.y ) );
				// Raz
				nX_debut = -1;
				nX_fin = -1;
			}
			// suivant
			i += 1;
		}
		// Recherche du minimum si on a deux maximums
		if ( nNb_maximums == 2 ) {
			ptMin[0] = (Point.Double)vPts_max.get( 0 );
			i = 0;
			ptA = new Point.Double( 0, 0 );
			while ( i < vHisto.size() && ptA.x < ((Point.Double)vPts_max.get( 1 )).x ) {
				ptA = (Point.Double)vHisto.get( i );
				if ( ptA.x >= ((Point.Double)vPts_max.get( 0 )).x  ){
					if ( ptA.y < ptMin[0].y ) {
						ptMin[0].x = ptA.x;
						ptMin[0].y = ptA.y;
					}
				}
				// suiv
				++i;
			}
		}
		return nNb_maximums;
	}// getNb_maximums_histogramme_vectorise
	
	
	/**
	 * Teste la bimodalité de l'histogramme
	 * @param vHisto : tableau de 256 entiers
	 * @return true si l'histogramme reçu est bimodal
	 */
	static public boolean bEst_bimodal( int[] vHisto, boolean bTracer ) {
		if ( bTracer ) {
			try {
				PrintWriter fOut = new PrintWriter( 
										new BufferedWriter( 
											new FileWriter("log_histogramme_brut.txt")));
				for ( int i = 0; i < vHisto.length; ++i ) {
					fOut.println( "" + i + " " + vHisto[i] );
				}
				fOut.close();
			}
			catch ( IOException e ) {
				IJ.log( "Erreur creation fichier" );
			}
		}
		final int ncNb_passes_max = 6;//6;
		int nNum_passe = 1;
		AbstractList vHisto_pts;// liste de "Point"
		// copie de l'histogramme reçu sur des entiers (passage en doubles)
		double[] vHisto_lisse = new double[vHisto.length];
		for ( int i = 0; i < vHisto.length; ++i ) {
			vHisto_lisse[ i ] = vHisto[ i ];
		}
		boolean bFin = false;
		final double ncSeuil_vectorisation = 1.0;// plus le seuil est important, moins la vectorisation est précise 
		int nNbMaximums = 0;
		boolean bEst_bimodal = false;
		boolean bEst_monomodal = false;
		while( nNum_passe <= ncNb_passes_max && ! bEst_bimodal && ! bEst_monomodal ) {
			double[] vHisto_retour = lisser_histogramme( vHisto_lisse, 6 );
			vHisto_lisse = (double[])vHisto_retour.clone();
			// ecriture eventuelle dans un fichier
			if ( bTracer ) {
				try {
					PrintWriter fOut = new PrintWriter( 
											new BufferedWriter( 
												new FileWriter("log_histogramme_lisse_passe_" + nNum_passe + ".txt")));
					for ( int i = 0; i < vHisto_lisse.length; ++i ) {
						fOut.println( "" + i + " " + vHisto_lisse[i] );
					}
					fOut.close();
				}
				catch ( IOException e ) {
					IJ.log( "Erreur creation fichier" );
				}
			}
			// vectorisation et comptage du nombre de maximums
			vHisto_pts = vectoriser_histogramme( vHisto_lisse, ncSeuil_vectorisation, bTracer );
			if ( bTracer ) {
				try {
					PrintWriter fOut = new PrintWriter( 
											new BufferedWriter( 
												new FileWriter("log_histogramme_vectorise.txt")));
					for ( int i = 0; i < vHisto_pts.size() ; ++i ) {
						fOut.println( "" + ((Point.Double)vHisto_pts.get(i)).x + " " + ((Point.Double)vHisto_pts.get(i)).y );
					}
					fOut.close();
				}
				catch ( IOException e ) {
					IJ.log( "Erreur creation fichier" );
				}
			}
			AbstractList vPts_max = new ArrayList();
			Point.Double ptMin = new Point.Double( 0, 0);
			Point.Double[] tabPts = new Point.Double[1];// pour passage par référence
			tabPts[0] = ptMin;// pour passage par référence
			nNbMaximums = getNb_maximums_histogramme_vectorise( vHisto_pts, vPts_max, tabPts, bTracer );
			ptMin = tabPts[0];// pour passage par référence
			if ( bTracer && nNbMaximums == 2 ) {
				/*IJ.log( " Pts max( " );
				for ( int i = 0; i < vPts_max.size(); ++i ) {
					IJ.log( " " + ((Point.Double)vPts_max.get(i)).x + ", " + ((Point.Double)vPts_max.get(i)).y );
				}
				IJ.log( " ) " );
				IJ.log( " Pt min( " + ptMin.x + " " + ptMin.y + ") " );*/
			}
			double nRatio_max_min = 0.0;
			if ( nNbMaximums == 2 ) {
				double h_max = Math.max( ((Point.Double)vPts_max.get( 0 )).y, ((Point.Double)vPts_max.get( 1 )).y );
				double h_min = ptMin.y;
				if ( h_min == 0 ) {
					h_min = 0.01;
				}
				nRatio_max_min = h_max / h_min;
				if ( bTracer ) {
					//IJ.log( "Apres passe " + nNum_passe + ", nb max == 2, ratio : " + nRatio_max_min + " " );
				}
			}
			bEst_bimodal = ( ( nNbMaximums == 2 ) && ( nRatio_max_min >= 1.5 ) );
			bEst_monomodal = ( nNbMaximums == 1 );
			// suivant
			nNum_passe += 1;
		}
		return bEst_bimodal;
	}// bEst_bimodal
	
	
}// Algos_images
